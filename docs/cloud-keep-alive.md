# BiteCheck — Cloudflare Keep-Alive Plan

Since Supabase pauses inactive projects after 7 days, we use a Cloudflare Worker to "ping" the database daily. Unlike GitHub Actions, this will run forever even if you don't commit code.

## 1. Preparation
Open your `A:/BiteCheck/local.properties` file and have these two values ready:
*   `SUPABASE_URL`
*   `SUPABASE_ANON_KEY`

## 2. Create the Cloudflare Worker
1.  Log in to the [Cloudflare Dashboard](https://dash.cloudflare.com/).
2.  Navigate to **Workers & Pages** in the sidebar.
3.  Click **Create application** -> **Create Worker**.
4.  Name it `bitecheck-keep-alive` and click **Deploy**.
5.  Click **Edit Code**.

## 3. The Worker Script
Delete everything in the editor and paste this exactly:

```javascript
export default {
  // This runs on your Cron schedule (once a day)
  async scheduled(event, env, ctx) {
    await performPing(env);
  },

  // This runs when you visit the URL in your browser (Manual Test)
  async fetch(request, env, ctx) {
    const result = await performPing(env);
    return new Response(result ? "Supabase Heartbeat: Success" : "Supabase Heartbeat: Failed", {
      headers: { "content-type": "text/plain" },
    });
  },
};

async function performPing(env) {
  const url = `${env.SUPABASE_URL}/rest/v1/profiles?select=count`;
  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'apikey': env.SUPABASE_ANON_KEY,
        'Authorization': `Bearer ${env.SUPABASE_ANON_KEY}`,
      },
    });

    if (response.ok) {
      console.log('Supabase Heartbeat: Success');
      return true;
    } else {
      console.error('Supabase Heartbeat: Failed', await response.text());
      return false;
    }
  } catch (err) {
    console.error('Ping Error:', err.message);
    return false;
  }
}
```
Click **Save and Deploy**.

## 4. Set Variables & Schedule
1.  Go back to the Worker's main page (click the name at the top left).
2.  Go to the **Settings** tab -> **Variables**.
3.  Under **Environment Variables**, click **Add variable**:
    *   Variable name: `SUPABASE_URL` | Value: (Your URL)
    *   Variable name: `SUPABASE_ANON_KEY` | Value: (Your Key) -> **Check "Secret"**.
4.  Click **Save and deploy**.
5.  Go to the **Triggers** tab -> **Cron Triggers**.
6.  Click **Add Cron Trigger**.
7.  Select **Custom** and enter: `0 0 * * *` (Runs every day at midnight).
8.  Click **Add Trigger**.

## 5. Verification
You can test it immediately:
1.  Simply visit your Worker's public URL in your browser.
2.  If the page says "Supabase Heartbeat: Success", it's working!
3.  You can also check the **Logs** tab in Cloudflare to see the history.
