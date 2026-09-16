/**
 * GET /api/config
 *
 * The one thing the static pages need from the deployment: where Supabase
 * lives, and which public key to talk to it with. Reading it here (instead of
 * baking it into a committed file) is what lets the same folder deploy to any
 * domain and any preview URL.
 *
 * Only PUBLIC values leave this endpoint. The anon/publishable key is designed
 * to be shipped to clients: row level security is what protects the data, and
 * the Android app carries the same key. The service-role key is never read
 * here at all, and `problem` never contains a secret, only a short explanation
 * of what is missing.
 */

const firstSet = (...names) => {
  for (const name of names) {
    const value = (process.env[name] || '').trim();
    if (value) return value;
  }
  return '';
};

export default function handler(request, response) {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    response.setHeader('Allow', 'GET, HEAD');
    return response.status(405).json({ configured: false, problem: 'Only GET is supported here.' });
  }

  const supabaseUrl = firstSet(
    'SUPABASE_URL',
    'NEXT_PUBLIC_SUPABASE_URL',
    'VITE_SUPABASE_URL',
    'PUBLIC_SUPABASE_URL'
  ).replace(/\/+$/, '');

  const supabaseAnonKey = firstSet(
    'SUPABASE_ANON_KEY',
    'SUPABASE_PUBLISHABLE_KEY',
    'NEXT_PUBLIC_SUPABASE_ANON_KEY',
    'VITE_SUPABASE_ANON_KEY',
    'PUBLIC_SUPABASE_ANON_KEY'
  );

  const serviceRole = firstSet('SUPABASE_SERVICE_ROLE_KEY', 'SUPABASE_SECRET_KEY');

  // The canonical public address of THIS site. Every email the site sends
  // carries it as the redirect, so a link can never be minted for whichever
  // preview deployment someone happened to open (that is how
  // "the URL keeps changing to a preview URL" happens: the address used to be
  // whatever hostname the page was served from).
  //
  // `VERCEL_PROJECT_PRODUCTION_URL` is Vercel's own system variable — the
  // project's production domain, the shortest custom domain or the
  // `*.vercel.app` one — so a preview deployment still builds production
  // links without any configuration. It carries no scheme, hence the
  // normalisation below; an explicitly set SITE_URL always wins.
  const rawSiteUrl = firstSet(
    'SITE_URL',
    'PUBLIC_SITE_URL',
    'NEXT_PUBLIC_SITE_URL',
    'APP_SITE_URL',
    'VERCEL_PROJECT_PRODUCTION_URL'
  ).replace(/\/+$/, '');
  const canonicalSiteUrl = rawSiteUrl
    ? (/^https?:\/\//i.test(rawSiteUrl) ? rawSiteUrl : 'https://' + rawSiteUrl)
    : '';

  const problems = [];
  if (!supabaseUrl) problems.push('SUPABASE_URL');
  if (!supabaseAnonKey) problems.push('SUPABASE_ANON_KEY (or SUPABASE_PUBLISHABLE_KEY)');

  response.setHeader('Cache-Control', 'no-store');
  response.setHeader('Content-Type', 'application/json; charset=utf-8');

  return response.status(200).json({
    configured: problems.length === 0,
    problem: problems.length
      ? 'Missing environment variable' + (problems.length > 1 ? 's' : '') + ': ' + problems.join(', ') + '.'
      : '',
    supabaseUrl,
    supabaseAnonKey,
    // Empty means "this page's own origin" — keep that working, it is what a
    // local preview and a brand-new deploy look like.
    siteUrl: canonicalSiteUrl,
    supportEmail: firstSet('SUPPORT_EMAIL', 'PRIVACY_CONTACT_EMAIL'),
    appDownloadUrl:
      firstSet('APP_DOWNLOAD_URL') || 'https://github.com/firefly-sylestia/Curio/releases/latest',
    repoUrl: firstSet('REPO_URL') || 'https://github.com/firefly-sylestia/Curio',
    siteName: firstSet('SITE_NAME') || 'Curio',
    // A flag, never the value: the account page can tell the member that
    // account deletion is unavailable instead of failing at the last tap.
    deletionEnabled: Boolean(serviceRole),
  });
}
