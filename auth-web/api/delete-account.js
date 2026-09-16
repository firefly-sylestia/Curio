/**
 * POST /api/delete-account
 *
 * The one privileged call on this site. It removes the signed-in member's
 * account, which means it holds the service-role key, which means it must trust
 * NOTHING that arrives in the request:
 *
 *   1. The caller proves who they are with their own access token, which is
 *      verified against GoTrue (`GET /auth/v1/user`). The id that call returns
 *      is the id that gets deleted. A `userId` in the body is ignored on
 *      purpose: honouring it would let anybody delete anybody.
 *   2. The typed confirmation is re-checked here. The dialog that fills it in
 *      is a UI affordance, not a security control.
 *   3. Only then is the account deleted with the service-role key, and every
 *      Curio table cascades off `auth.users` (schema §1, §4, §5c, §5d).
 *
 * Nothing about the account (email, id, key) is ever logged or returned.
 */

const SERVICE_ROLE_ALIASES = ['SUPABASE_SERVICE_ROLE_KEY', 'SUPABASE_SECRET_KEY'];
const ANON_ALIASES = [
  'SUPABASE_ANON_KEY',
  'SUPABASE_PUBLISHABLE_KEY',
  'NEXT_PUBLIC_SUPABASE_ANON_KEY',
  'VITE_SUPABASE_ANON_KEY',
];
const URL_ALIASES = ['SUPABASE_URL', 'NEXT_PUBLIC_SUPABASE_URL', 'VITE_SUPABASE_URL'];

const firstSet = (...names) => {
  for (const name of names) {
    const value = (process.env[name] || '').trim();
    if (value) return value;
  }
  return '';
};

/** The bearer token, with no other shape accepted. */
function bearer(request) {
  const header = request.headers.authorization || request.headers.Authorization || '';
  const match = /^Bearer\s+(.+)$/i.exec(String(header).trim());
  return match ? match[1].trim() : '';
}

/** The parsed JSON body, whatever the runtime handed us. */
async function body(request) {
  if (request.body && typeof request.body === 'object') return request.body;
  if (typeof request.body === 'string' && request.body) {
    try {
      return JSON.parse(request.body);
    } catch (error) {
      return {};
    }
  }
  // Some runtimes only expose a stream here.
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  if (!chunks.length) return {};
  try {
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } catch (error) {
    return {};
  }
}

export default async function handler(request, response) {
  response.setHeader('Cache-Control', 'no-store');

  if (request.method !== 'POST') {
    response.setHeader('Allow', 'POST');
    return response.status(405).json({ code: 'method_not_allowed', message: 'Only POST is supported here.' });
  }

  const supabaseUrl = firstSet(...URL_ALIASES).replace(/\/+$/, '');
  const anonKey = firstSet(...ANON_ALIASES);
  const serviceKey = firstSet(...SERVICE_ROLE_ALIASES);

  if (!supabaseUrl || !anonKey) {
    return response.status(503).json({
      code: 'not_configured',
      message: 'This site is not connected to its Curio server yet, so the account cannot be deleted.',
    });
  }

  if (!serviceKey) {
    return response.status(503).json({
      code: 'deletion_unavailable',
      message:
        'Account deletion is not switched on for this deployment. Add the server key in Vercel, or write to the support address and it will be done for you.',
    });
  }

  const token = bearer(request);
  if (!token) {
    return response.status(401).json({
      code: 'no_session',
      message: 'Sign in again, then delete your account.',
    });
  }

  const payload = await body(request);
  if (String(payload && payload.confirm).trim().toUpperCase() !== 'DELETE') {
    return response.status(400).json({
      code: 'confirm_required',
      message: 'Type DELETE to confirm, then try again.',
    });
  }

  // Who is this, really? Only the identity GoTrue returns from the caller's own
  // token is trusted.
  let user = null;
  try {
    const whoami = await fetch(`${supabaseUrl}/auth/v1/user`, {
      headers: { apikey: anonKey, Authorization: `Bearer ${token}` },
    });
    if (whoami.ok) user = await whoami.json();
  } catch (error) {
    return response.status(502).json({
      code: 'server_unreachable',
      message: 'The Curio server could not be reached. Try again in a moment.',
    });
  }

  if (!user || !user.id) {
    return response.status(401).json({
      code: 'stale_session',
      message: 'That session has expired. Sign in again, then delete your account.',
    });
  }

  let removed;
  try {
    removed = await fetch(`${supabaseUrl}/auth/v1/admin/users/${encodeURIComponent(user.id)}`, {
      method: 'DELETE',
      headers: {
        apikey: serviceKey,
        Authorization: `Bearer ${serviceKey}`,
        'Content-Type': 'application/json',
      },
    });
  } catch (error) {
    return response.status(502).json({
      code: 'server_unreachable',
      message: 'The Curio server could not be reached. Nothing was deleted. Try again in a moment.',
    });
  }

  if (!removed.ok) {
    // The account was not removed, so say so plainly and keep the server's own
    // text out of the page.
    return response.status(removed.status === 404 ? 404 : 500).json({
      code: removed.status === 404 ? 'already_gone' : 'delete_failed',
      message:
        removed.status === 404
          ? 'That account no longer exists. Sign in again to check.'
          : 'The account could not be deleted. Nothing changed. Try again, or write to the support address.',
    });
  }

  return response.status(200).json({ ok: true });
}
