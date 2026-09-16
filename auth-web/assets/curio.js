/* ═══════════════════════════════════════════════════════════════════════════
   Curio account site: the only script.

   No dependencies, no build step, no inline scripts (the CSP forbids them).
   Every page declares what it is with <body data-page="...">, and this file
   wires that page. The Supabase URL and anon key come from /api/config, so
   nothing is committed here.

   The shape of the work:
     1. load the config, or explain that the site is not configured yet
     2. talk to GoTrue (Supabase Auth) over fetch
     3. normalise whatever the email link delivered (fragment, token_hash, error)
     4. show exactly one state, with exactly one next action
   ═══════════════════════════════════════════════════════════════════════════ */

/* ── tiny DOM helpers ────────────────────────────────────────────────────── */

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

/** getElementById that complains in the console when the markup drifts. */
function need(id) {
  const node = document.getElementById(id);
  if (!node) console.error('[curio] missing element #' + id + ' on page ' + pageName());
  return node;
}

const pageName = () => document.body.dataset.page || 'home';

/* ── config ──────────────────────────────────────────────────────────────── */

let configRequest = null;

function loadConfig() {
  if (!configRequest) {
    configRequest = fetch('/api/config', { cache: 'no-store' })
      .then((response) => (response.ok ? response.json() : Promise.reject(new Error('config ' + response.status))))
      .then((data) => ({
        configured: Boolean(data && data.configured),
        problem: (data && data.problem) || '',
        supabaseUrl: String((data && data.supabaseUrl) || '').replace(/\/+$/, ''),
        supabaseAnonKey: (data && data.supabaseAnonKey) || '',
        supportEmail: (data && data.supportEmail) || '',
        appDownloadUrl: (data && data.appDownloadUrl) || '',
        repoUrl: (data && data.repoUrl) || '',
        siteName: (data && data.siteName) || 'Curio',
      }))
      .catch(() => ({
        configured: false,
        problem: 'This site could not reach its own configuration endpoint.',
        supabaseUrl: '',
        supabaseAnonKey: '',
        supportEmail: '',
        appDownloadUrl: '',
        repoUrl: '',
        siteName: 'Curio',
      }));
  }
  return configRequest;
}

/**
 * The state every flow page shows when the deployment has no Supabase
 * variables yet. It is a first-class state, not an error: a fresh clone and a
 * half-finished deploy both look like this, and both must explain themselves.
 */
function paintSetupNotice(config) {
  const page = need('page') || $('main');
  if (!page) return;
  $$('.state', page).forEach((state) => state.classList.remove('show'));
  const card = document.createElement('section');
  card.className = 'card rise';
  const title = document.createElement('h2');
  title.textContent = 'This site is not connected yet';
  const body = document.createElement('p');
  body.className = 'muted';
  body.textContent =
    'The Curio account service is deployed without its Supabase settings, so there is nothing to sign in to here yet. ' +
    'Everything in the app keeps working: this page is only the web side of your account.';
  const detail = document.createElement('p');
  detail.className = 'faint';
  detail.textContent = config.problem || 'Add SUPABASE_URL and SUPABASE_ANON_KEY to the deployment, then reload.';
  card.append(title, body, detail);
  page.prepend(card);
}

/* ── errors and copy ─────────────────────────────────────────────────────── */

class CurioError extends Error {
  constructor(message, { status = 0, code = '' } = {}) {
    super(message);
    this.name = 'CurioError';
    this.status = status;
    this.code = code;
  }
}

/**
 * Turns a failure into one sentence a member can act on. Server vocabulary
 * never reaches the page: the raw detail stays in the console.
 */
function friendly(error, context = '') {
  const code = String((error && error.code) || '').toLowerCase();
  const raw = String((error && error.message) || '');
  const lower = raw.toLowerCase();
  const status = Number((error && error.status) || 0);
  const has = (...needles) => needles.some((needle) => code.includes(needle) || lower.includes(needle));

  if (error && error.name === 'TypeError') {
    return 'No connection. Check your network, then try again.';
  }
  if (status === 429 || has('over_email_send_rate_limit', 'rate limit', 'too many')) {
    return 'That is a lot of emails in a row. Wait a minute, then try once more.';
  }
  if (has('otp_expired', 'token has expired', 'expired', 'invalid or has expired')) {
    return 'That link has expired or was already used. Ask for a new one below.';
  }
  if (has('user_already_exists', 'already registered', 'email_exists')) {
    return 'That address already has a Curio account. Sign in instead, or reset its password.';
  }
  if (has('email_not_confirmed', 'not confirmed')) {
    return 'That address has not been confirmed yet. Use the link in your inbox first.';
  }
  if (has('invalid_credentials', 'invalid_grant', 'invalid login')) {
    return 'That email and password do not match. Check them, or reset your password.';
  }
  if (has('user_not_found', 'no user found')) {
    return 'There is no Curio account with that address.';
  }
  if (has('same_password')) {
    return 'That is your current password. Choose a different one.';
  }
  if (has('weak_password', 'password should be')) {
    return 'Choose a longer password: at least 8 characters.';
  }
  if (has('signup_disabled', 'email_provider_disabled', 'provider is disabled')) {
    return 'New accounts are turned off for this project right now.';
  }
  if (has('validation_failed', 'unable to validate email')) {
    return 'That does not look like a complete email address.';
  }
  if (context === 'delete' && status === 401) {
    return 'Your session has expired. Sign in again, then delete your account.';
  }
  console.warn('[curio]', context, error);
  return 'Something went wrong. Try again in a moment.';
}

/* ── the session ─────────────────────────────────────────────────────────── */

const SESSION_KEY = 'curio.web.session';

const session = {
  read() {
    try {
      const value = JSON.parse(localStorage.getItem(SESSION_KEY) || 'null');
      return value && value.access_token ? value : null;
    } catch (error) {
      return null;
    }
  },
  write(data) {
    if (!data || !data.access_token) return;
    const expiresIn = Number(data.expires_in || 3600);
    try {
      localStorage.setItem(
        SESSION_KEY,
        JSON.stringify({
          access_token: data.access_token,
          refresh_token: data.refresh_token || '',
          expires_at: Date.now() + Math.max(0, expiresIn - 30) * 1000,
          user_id: (data.user && data.user.id) || '',
          email: (data.user && data.user.email) || '',
        })
      );
    } catch (error) {
      console.warn('[curio] session not stored', error);
    }
  },
  /** Adds fields to a stored session without touching its expiry. */
  patch(fields) {
    const current = session.read();
    if (!current) return;
    try {
      localStorage.setItem(SESSION_KEY, JSON.stringify({ ...current, ...fields }));
    } catch (error) {
      /* nothing to do */
    }
  },
  clear() {
    try {
      localStorage.removeItem(SESSION_KEY);
    } catch (error) {
      /* nothing to do */
    }
  },
  fresh(current) {
    return Boolean(current && current.expires_at && current.expires_at > Date.now());
  },
};

/* ── the GoTrue client ───────────────────────────────────────────────────── */

class Auth {
  constructor(config) {
    this.config = config;
  }

  url(path, query) {
    const base = this.config.supabaseUrl + path;
    return query ? base + '?' + new URLSearchParams(query).toString() : base;
  }

  async request(path, options = {}) {
    const { method = 'GET', body, token, query } = options;
    const headers = {
      apikey: this.config.supabaseAnonKey,
      'Content-Type': 'application/json',
    };
    headers.Authorization = 'Bearer ' + (token || this.config.supabaseAnonKey);

    let response;
    try {
      response = await fetch(this.url(path, query), {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (error) {
      throw new CurioError('network', { code: 'network' });
    }

    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (error) {
        data = { msg: text.slice(0, 200) };
      }
    }

    if (!response.ok) {
      const message = (data && (data.msg || data.error_description || data.error || data.message)) || 'request failed';
      const code = (data && (data.error_code || data.code)) || '';
      throw new CurioError(message, { status: response.status, code });
    }
    return data;
  }

  /* ── account creation and sign in ── */

  signUp(email, password, redirectTo) {
    return this.request('/auth/v1/signup', {
      method: 'POST',
      query: { redirect_to: redirectTo },
      body: { email, password },
    });
  }

  signIn(email, password) {
    return this.request('/auth/v1/token', {
      method: 'POST',
      query: { grant_type: 'password' },
      body: { email, password },
    });
  }

  refresh(refreshToken) {
    return this.request('/auth/v1/token', {
      method: 'POST',
      query: { grant_type: 'refresh_token' },
      body: { refresh_token: refreshToken },
    });
  }

  logout(token) {
    return this.request('/auth/v1/logout', { method: 'POST', token, body: {} });
  }

  /* ── emails ── */

  /** The password reset email. The link lands on redirectTo with the token. */
  recover(email, redirectTo) {
    return this.request('/auth/v1/recover', {
      method: 'POST',
      query: { redirect_to: redirectTo },
      body: { email },
    });
  }

  /** A one-tap sign-in link. createUser mirrors Supabase's shouldCreateUser. */
  magicLink(email, redirectTo, createUser) {
    return this.request('/auth/v1/otp', {
      method: 'POST',
      query: { redirect_to: redirectTo },
      body: { email, create_user: Boolean(createUser) },
    });
  }

  /**
   * Sends the signup confirmation email again.
   *
   * `redirect_to` matters here as much as it does on signup: without it the
   * resent link goes to the project's Site URL, which is the whole reason a
   * member ends up on `http://localhost:3000`. GoTrue reads the same query
   * parameter for `/resend` as for `/signup`.
   */
  resendConfirmation(email, redirectTo) {
    return this.request('/auth/v1/resend', {
      method: 'POST',
      query: { redirect_to: redirectTo },
      body: { type: 'signup', email },
    });
  }

  /** Exchanges a ?token_hash=&type= link for a session. */
  verify(tokenHash, type) {
    return this.request('/auth/v1/verify', {
      method: 'POST',
      body: { token_hash: tokenHash, type: type || 'email' },
    });
  }

  /* ── the signed-in user ── */

  me(token) {
    return this.request('/auth/v1/user', { token });
  }

  updatePassword(token, password) {
    return this.request('/auth/v1/user', { method: 'PUT', token, body: { password } });
  }

  /** A stored session, refreshed if it is at or near its expiry. */
  async liveSession() {
    const current = session.read();
    if (!current) return null;
    if (session.fresh(current)) return current;
    if (!current.refresh_token) {
      session.clear();
      return null;
    }
    try {
      const refreshed = await this.refresh(current.refresh_token);
      session.write(refreshed);
      return session.read();
    } catch (error) {
      session.clear();
      return null;
    }
  }
}

/* ── what the email link delivered ───────────────────────────────────────── */

/**
 * Normalises the three shapes a Supabase email link can arrive in:
 *   - a fragment session (#access_token=...), the classic /verify redirect
 *   - a ?token_hash=&type= pair, the modern template form
 *   - an error fragment (error_code, error_description)
 * The fragment is never sent to the server, which is exactly why it is used.
 */
function readLink() {
  const hash = new URLSearchParams((location.hash || '').replace(/^#/, ''));
  const query = new URLSearchParams(location.search || '');
  const pick = (key) => hash.get(key) || query.get(key) || '';
  return {
    accessToken: pick('access_token'),
    refreshToken: pick('refresh_token'),
    expiresIn: pick('expires_in'),
    type: pick('type'),
    tokenHash: pick('token_hash') || pick('token'),
    code: pick('code'),
    error: pick('error') || pick('error_code'),
    errorCode: pick('error_code'),
    errorDescription: pick('error_description') || pick('error'),
    hasSomething: Boolean(
      hash.toString() || query.get('token_hash') || query.get('token') || query.get('code') || query.get('error')
    ),
  };
}

/** Drops the tokens out of the address bar and out of the history entry. */
function cleanUrl() {
  if (!location.hash && !location.search) return;
  history.replaceState(null, '', location.pathname);
}

/** The address a link in an email should come back to. */
const siteUrl = (path) => location.origin + path;

/* ── carrying an address between pages ───────────────────────────────────── */

/**
 * The sign-in page's "this address is new" path continues on /signup, and the
 * address already typed travels with it. sessionStorage rather than the URL:
 * an email in a query string ends up in history and in bookmarks.
 */
const HANDOFF_KEY = 'curio.signup.email';

function carryToSignup(email) {
  try {
    sessionStorage.setItem(HANDOFF_KEY, email);
  } catch (error) {
    /* the create page simply starts with an empty field */
  }
}

function takeCarriedEmail() {
  try {
    const email = sessionStorage.getItem(HANDOFF_KEY) || '';
    sessionStorage.removeItem(HANDOFF_KEY);
    return email;
  } catch (error) {
    return '';
  }
}

/* ── small UI helpers ────────────────────────────────────────────────────── */

/** Shows one state and hides its siblings. Every state lives in the markup. */
function showState(name) {
  const states = $$('.state');
  let shown = null;
  states.forEach((state) => {
    const match = state.dataset.state === name;
    state.classList.toggle('show', match);
    if (match) shown = state;
  });
  if (!shown) console.error('[curio] no state named ' + name);
  return shown;
}

function notice(el, kind, text) {
  if (!el) return;
  if (!text) {
    el.className = 'notice';
    el.textContent = '';
    return;
  }
  el.className = 'notice show ' + (kind || 'info');
  el.textContent = text;
}

function busy(button, isBusy, busyLabel) {
  if (!button) return;
  if (isBusy) {
    button.dataset.label = button.textContent;
    button.textContent = busyLabel || 'Working…';
    button.setAttribute('aria-busy', 'true');
    button.disabled = true;
  } else {
    if (button.dataset.label) button.textContent = button.dataset.label;
    button.removeAttribute('aria-busy');
    button.disabled = false;
  }
}

const value = (id) => (need(id) ? need(id).value.trim() : '');
const emailLooksReal = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

/** Fills every [data-support-email] slot, or hides it when there is none. */
function paintContact(config) {
  const slots = $$('[data-support-email]');
  slots.forEach((slot) => {
    if (config.supportEmail) {
      const link = document.createElement('a');
      link.href = 'mailto:' + config.supportEmail;
      link.textContent = config.supportEmail;
      slot.replaceChildren(link);
      slot.classList.remove('muted');
    } else {
      slot.textContent =
        'Support is reachable inside Curio: open Profile, then Support & diagnostics.';
    }
  });
  $$('[data-app-download]').forEach((link) => {
    if (config.appDownloadUrl) link.href = config.appDownloadUrl;
    else link.classList.add('hidden');
  });
}

/* ── the pages ───────────────────────────────────────────────────────────── */

/**
 * Consumes a link that carries a session, for the pages that only need to say
 * "done". Returns { ok } plus the loaded session on success.
 */
async function consumeLink(auth, typeHint) {
  const link = readLink();

  if (link.accessToken) {
    session.write({
      access_token: link.accessToken,
      refresh_token: link.refreshToken,
      expires_in: link.expiresIn,
      user: null,
    });
    cleanUrl();
    // The fragment carries the tokens, not the identity. One authenticated
    // read fills in who this is, so the page can name them. A failure here is
    // cosmetic: the session itself is already usable.
    try {
      const me = await auth.me(link.accessToken);
      if (me) session.patch({ user_id: me.id || '', email: me.email || '' });
    } catch (error) {
      console.warn('[curio] could not read the signed-in identity', error);
    }
    return { ok: true, session: session.read() };
  }

  if (link.tokenHash) {
    const wanted = link.type || typeHint;
    const ladder = [wanted, 'email', 'signup', 'magiclink', 'recovery', 'invite'].filter(
      (candidate, index, all) => candidate && all.indexOf(candidate) === index
    );
    let last = null;
    for (const type of ladder) {
      try {
        const data = await auth.verify(link.tokenHash, type);
        session.write(data);
        cleanUrl();
        return { ok: true, session: session.read() };
      } catch (error) {
        last = error;
      }
    }
    cleanUrl();
    return { ok: false, error: last, reason: 'expired' };
  }

  if (link.error || link.errorCode) {
    cleanUrl();
    return {
      ok: false,
      error: new CurioError(link.errorDescription || 'link failed', { code: link.errorCode }),
      reason: 'expired',
    };
  }

  return { ok: false, error: null, reason: 'nothing' };
}

/**
 * The confirmation page: consume the link, then keep one "send a new link"
 * form on screen for the two cases that need it (an expired link, and a
 * visitor who opened the page directly).
 */
async function initConfirm(auth) {
  const outcome = await consumeLink(auth, 'signup');

  if (outcome.ok) {
    showState('done');
    const who = need('done-email');
    if (who) who.textContent = (outcome.session && outcome.session.email) || '';
    const resend = need('resend-block');
    if (resend) resend.classList.add('hidden');
    return;
  }

  if (outcome.reason === 'nothing') {
    // Someone opened this page directly: the honest answer is "use the link
    // from your inbox", with the resend form right there.
    showState('waiting');
    return;
  }

  showState('expired');
  notice(need('expired-status'), 'warn', friendly(outcome.error, 'confirm'));

  const form = need('resend-form');
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = need('resend-button');
    const email = value('resend-email');
    const status = need('resend-status');
    if (!emailLooksReal(email)) {
      notice(status, 'warn', 'Check that email address.');
      return;
    }
    busy(button, true, 'Sending…');
    notice(status, null);
    try {
      await auth.resendConfirmation(email, siteUrl('/confirm'));
      notice(status, 'ok', 'A fresh confirmation email is on its way to ' + email + '.');
    } catch (error) {
      notice(status, 'bad', friendly(error, 'resend'));
    } finally {
      busy(button, false);
    }
  });
}

async function initLink(auth) {
  const outcome = await consumeLink(auth, 'magiclink');

  if (outcome.ok) {
    showState('done');
    const who = need('done-email');
    if (who) who.textContent = (outcome.session && outcome.session.email) || '';
    return;
  }

  if (outcome.reason === 'nothing') {
    showState('waiting');
    return;
  }

  showState('expired');
  notice(need('expired-status'), 'warn', friendly(outcome.error, 'link'));
}

async function initReset(auth) {
  const link = readLink();

  // 1. The email link: it carries a recovery session or a token_hash.
  if (link.accessToken || link.tokenHash || link.error) {
    const outcome = await consumeLink(auth, 'recovery');
    if (outcome.ok) {
      showState('choose');
      return;
    }
    showState('request');
    notice(
      need('request-status'),
      'warn',
      outcome.error ? friendly(outcome.error, 'reset') : 'Open the reset link from your inbox to choose a new password.'
    );
    return;
  }

  // 2. The page itself: ask for the email.
  showState('request');

  const form = need('request-form');
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = need('request-button');
    const email = value('request-email');
    if (!emailLooksReal(email)) {
      notice(need('request-status'), 'warn', 'Check that email address.');
      return;
    }
    busy(button, true, 'Sending…');
    notice(need('request-status'), null);
    try {
      await auth.recover(email, siteUrl('/reset'));
      const sent = need('request-sent');
      if (sent) sent.textContent = email;
      showState('sent');
    } catch (error) {
      notice(need('request-status'), 'bad', friendly(error, 'recover'));
    } finally {
      busy(button, false);
    }
  });

  // 3. The choose-a-password form (only ever reached with a live session).
  const choose = need('choose-form');
  choose.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = need('choose-button');
    const status = need('choose-status');
    const password = need('new-password').value;
    const again = need('new-password-2').value;

    if (password.length < 8) {
      notice(status, 'warn', 'Use at least 8 characters.');
      return;
    }
    if (password !== again) {
      notice(status, 'warn', 'Those two passwords are not the same.');
      return;
    }

    const live = await auth.liveSession();
    if (!live) {
      notice(status, 'bad', 'That reset link has expired. Ask for a new one.');
      showState('request');
      return;
    }

    busy(button, true, 'Saving…');
    notice(status, null);
    try {
      await auth.updatePassword(live.access_token, password);
      session.clear();
      showState('saved');
    } catch (error) {
      notice(status, 'bad', friendly(error, 'password'));
    } finally {
      busy(button, false);
    }
  });
}

async function initSignin(auth) {
  showState('form');

  const next = new URLSearchParams(location.search).get('next') || '/account';

  // Link sign-in. Ticking "this address is new" hands the member to /signup,
  // where the password is chosen: a link that signs someone in has no password
  // to set, so it must never be how an account starts.
  const linkForm = need('link-form');
  const createToggle = need('link-create');
  const linkButton = need('link-button');
  const paintLinkButton = () => {
    if (!linkButton) return;
    linkButton.textContent = createToggle && createToggle.checked ? 'Create my account' : 'Send me a link';
  };
  if (createToggle) createToggle.addEventListener('change', paintLinkButton);
  paintLinkButton();

  linkForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const email = value('link-email');
    if (!emailLooksReal(email)) {
      notice(need('link-status'), 'warn', 'Check that email address.');
      return;
    }
    if (createToggle && createToggle.checked) {
      carryToSignup(email);
      location.assign('/signup');
      return;
    }
    busy(linkButton, true, 'Sending…');
    notice(need('link-status'), null);
    try {
      await auth.magicLink(email, siteUrl('/link'), false);
      const sent = need('link-sent');
      if (sent) sent.textContent = email;
      showState('sent');
    } catch (error) {
      notice(need('link-status'), 'bad', friendly(error, 'magiclink'));
    } finally {
      busy(linkButton, false);
    }
  });

  // Password sign-in.
  const passwordForm = need('password-form');
  passwordForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = need('password-button');
    const email = value('password-email');
    const password = need('password-value').value;
    if (!emailLooksReal(email) || password.length < 1) {
      notice(need('password-status'), 'warn', 'Enter your email and password.');
      return;
    }
    busy(button, true, 'Signing in…');
    notice(need('password-status'), null);
    try {
      const data = await auth.signIn(email, password);
      session.write(data);
      location.assign(next);
    } catch (error) {
      notice(need('password-status'), 'bad', friendly(error, 'signin'));
      busy(button, false);
    }
  });
}

/**
 * Creating an account: email, password, repeat, terms. The password is set
 * HERE, before any email goes out, so a confirmed account already has one and
 * the sign-in page's password form (and the app) can use it from the start.
 */
async function initSignup(auth) {
  showState('form');

  const emailField = need('signup-email');
  const carried = takeCarriedEmail();
  if (emailField && carried) emailField.value = carried;

  const form = need('signup-form');
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = need('signup-button');
    const status = need('signup-status');
    const email = value('signup-email');
    const password = need('signup-password').value;
    const again = need('signup-password-2').value;
    const agreed = need('signup-terms').checked;

    if (!emailLooksReal(email)) {
      notice(status, 'warn', 'Check that email address.');
      return;
    }
    if (password.length < 6) {
      notice(status, 'warn', 'Use a password of at least 6 characters.');
      return;
    }
    if (password !== again) {
      notice(status, 'warn', 'Those two passwords are not the same.');
      return;
    }
    if (!agreed) {
      notice(status, 'warn', 'Agree to the terms before creating your account.');
      return;
    }

    busy(button, true, 'Creating…');
    notice(status, null);
    try {
      const data = await auth.signUp(email, password, siteUrl('/confirm'));
      // A project with email confirmation off answers with a session, so the
      // account is ready to use the moment it exists.
      if (data && data.access_token) {
        session.write(data);
        location.assign('/account');
        return;
      }
      const sent = need('signup-sent');
      if (sent) sent.textContent = email;
      showState('sent');
    } catch (error) {
      notice(status, 'bad', friendly(error, 'signup'));
    } finally {
      busy(button, false);
    }
  });
}

async function initAccount(auth, config) {
  const live = await auth.liveSession();
  if (!live) {
    showState('signed-out');
    return;
  }

  showState('signed-in');
  const emailSlots = $$('[data-account-email]');
  emailSlots.forEach((slot) => {
    slot.textContent = live.email || '';
  });

  // The public half of the profile row, with the member's own token: RLS is
  // the only thing deciding what comes back.
  try {
    const response = await fetch(
      auth.url('/rest/v1/profiles', {
        select: 'username,display_name,avatar_style,online_mode_enabled,created_at',
        id: 'eq.' + live.user_id,
        limit: '1',
      }),
      {
        headers: {
          apikey: auth.config.supabaseAnonKey,
          Authorization: 'Bearer ' + live.access_token,
        },
      }
    );
    const rows = response.ok ? await response.json() : [];
    const profile = Array.isArray(rows) ? rows[0] : null;
    if (profile) paintProfile(profile);
  } catch (error) {
    console.warn('[curio] profile read failed', error);
  }

  // Counts are "live" numbers: a card lives 24 hours, so this is what is on the
  // wall right now, which is the honest label for it.
  await Promise.all([
    countRows(auth, live, '/rest/v1/community_cards', 'owner', 'stat-posts'),
    countRows(auth, live, '/rest/v1/community_comments', 'author', 'stat-replies'),
  ]);

  // Sign out.
  const signOut = need('sign-out');
  signOut.addEventListener('click', async () => {
    busy(signOut, true, 'Signing out…');
    try {
      await auth.logout(live.access_token);
    } catch (error) {
      /* a session that cannot be revoked server-side is still cleared here */
    }
    session.clear();
    showState('signed-out');
  });

  // Deleting the account: a typed confirmation, then a server call that
  // re-checks the identity before it removes anything.
  const dialog = need('delete-dialog');
  const confirmWord = need('delete-confirm-word');
  const deleteButton = need('delete-button');
  const openDelete = need('delete-start');
  const status = need('delete-status');

  // The deployment says whether it can delete anything at all. Saying so up
  // front is better than letting the last tap fail on a server it cannot reach.
  if (!config.deletionEnabled) {
    openDelete.disabled = true;
    notice(
      status,
      'warn',
      'Account deletion is not switched on for this deployment yet. Write to the support address and it will be done for you.'
    );
  }

  openDelete.addEventListener('click', () => {
    confirmWord.value = '';
    deleteButton.disabled = true;
    notice(status, null);
    if (typeof dialog.showModal === 'function') dialog.showModal();
    else dialog.setAttribute('open', '');
  });

  need('delete-cancel').addEventListener('click', () => dialog.close());

  confirmWord.addEventListener('input', () => {
    deleteButton.disabled = confirmWord.value.trim().toUpperCase() !== 'DELETE';
  });

  deleteButton.addEventListener('click', async () => {
    busy(deleteButton, true, 'Deleting…');
    notice(status, null);
    try {
      const response = await fetch('/api/delete-account', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + live.access_token,
        },
        body: JSON.stringify({ confirm: 'DELETE' }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new CurioError(data.message || 'delete failed', {
          status: response.status,
          code: data.code || '',
        });
      }
      session.clear();
      dialog.close();
      showState('deleted');
    } catch (error) {
      notice(status, 'bad', friendly(error, 'delete'));
      busy(deleteButton, false);
      confirmWord.value = '';
      deleteButton.disabled = true;
    }
  });

  paintContact(config);
}

function paintProfile(profile) {
  const display = (profile.display_name || '').trim();
  const username = (profile.username || '').trim();
  const label = display || username;
  const disc = need('profile-disc');
  if (disc) {
    disc.textContent = (label || '?')
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }
  const name = need('profile-name');
  if (name) name.textContent = label || 'Curio member';
  const handle = need('profile-handle');
  if (handle) handle.textContent = username ? '@' + username : '@' + 'unnamed';
  const avatar = need('profile-avatar');
  if (avatar) avatar.textContent = 'Portrait ' + (Number(profile.avatar_style) + 1) + ' of 28, chosen in the app';
  const online = need('profile-online');
  if (online) {
    online.textContent = profile.online_mode_enabled ? 'Online mode is on' : 'Online mode is off';
    online.classList.toggle('on', Boolean(profile.online_mode_enabled));
  }
  const since = need('profile-since');
  if (since && profile.created_at) {
    const when = new Date(profile.created_at);
    if (!Number.isNaN(when.getTime())) {
      since.textContent = when.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    }
  }
}

async function countRows(auth, live, path, column, slotId) {
  const slot = need(slotId);
  if (!slot) return;
  try {
    const response = await fetch(auth.url(path, { select: 'id', [column]: 'eq.' + live.user_id, limit: '200' }), {
      headers: {
        apikey: auth.config.supabaseAnonKey,
        Authorization: 'Bearer ' + live.access_token,
      },
    });
    const rows = response.ok ? await response.json() : [];
    slot.textContent = Array.isArray(rows) ? String(rows.length) : '0';
  } catch (error) {
    slot.textContent = '0';
  }
}

/* ── boot ────────────────────────────────────────────────────────────────── */

const PAGES = {
  confirm: initConfirm,
  link: initLink,
  reset: initReset,
  signin: initSignin,
  signup: initSignup,
  account: initAccount,
  // The legal pages and support must render even without a config: a privacy
  // URL that depends on an API key is not a privacy URL.
  legal: async (auth, config) => paintContact(config),
  support: async (auth, config) => paintContact(config),
  home: async (auth, config) => paintContact(config),
};

async function boot() {
  const config = await loadConfig();
  const page = pageName();
  const init = PAGES[page] || PAGES.home;

  if (!config.configured && page !== 'legal' && page !== 'support' && page !== 'home') {
    paintSetupNotice(config);
    return;
  }

  const auth = new Auth(config);
  try {
    // The account page needs the contact slots painted before the session
    // resolution paints the rest, so it is handed the config too.
    await init(auth, config);
  } catch (error) {
    console.error('[curio] page failed', error);
    const status = need('page-status') || $('.notice');
    notice(status, 'bad', friendly(error, page));
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
