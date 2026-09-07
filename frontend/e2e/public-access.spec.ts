import { expect, test } from '@playwright/test';

/**
 * GH-159: `example.spec.ts` asserted the page title and had its only other test body commented
 * out, so the e2e suite proved nothing about the application.
 *
 * <p>What is here is deliberately the part that runs without a backend: the public surface, the
 * client-side form rules, and the guards' redirects. Those are real behaviour and they are
 * checkable from a dev server alone.
 *
 * <p>What is NOT here is the login -> apply -> admin approve path the issue also asks for. That
 * needs Postgres, Redis and MinIO up and seeded, which no CI job currently provides. Writing it
 * against a stubbed backend would assert the stub, not the system, so it waits for an
 * environment that can actually run it.
 */

test.describe('public pages', () => {
  test('home page loads and is titled', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle(/Student Accommodation Management/);
    // Redirected from '' to /home by the router's first route.
    await expect(page).toHaveURL(/\/home$/);
  });

  test('buildings can be browsed without signing in', async ({ page }) => {
    await page.goto('/buildings');

    await expect(page).toHaveURL(/\/buildings$/);
    await expect(page.locator('body')).not.toContainText('Sign in to continue');
  });

  test('an unknown path falls back to home rather than erroring', async ({ page }) => {
    await page.goto('/this-route-does-not-exist');

    await expect(page).toHaveURL(/\/home$/);
  });
});

test.describe('login form', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('renders an email and password field', async ({ page }) => {
    await expect(page.getByLabel(/email/i)).toBeVisible();
    await expect(page.getByLabel(/password/i).first()).toBeVisible();
  });

  /** The button is disabled rather than submitting and failing, so an empty form cannot be sent. */
  test('keeps submit disabled until the form is filled in', async ({ page }) => {
    const submit = page.getByRole('button', { name: /sign in|log ?in/i }).first();

    await expect(submit).toBeDisabled();

    await page.getByLabel(/email/i).fill('student@example.com');
    await page.getByLabel(/password/i).first().fill('a-password');

    await expect(submit).toBeEnabled();
  });

  /**
   * Asserted through the submit button rather than the `mat-error` text: the error element is in
   * the DOM as soon as the value is invalid, but Material keeps the subscript hidden until the
   * field is in an error state, so its visibility is a detail of the component library. Whether
   * the form can be sent is the behaviour that matters.
   */
  test('will not submit an invalid email address', async ({ page }) => {
    const submit = page.getByRole('button', { name: /sign in|log ?in/i }).first();

    await page.getByLabel(/email/i).fill('not-an-email');
    await page.getByLabel(/password/i).first().fill('a-long-enough-password');

    await expect(submit).toBeDisabled();
  });
});

test.describe('guards', () => {
  test('the dashboard is not reachable while signed out', async ({ page }) => {
    await page.goto('/dashboard');

    await expect(page).toHaveURL(/\/login/);
  });

  test('the admin area is not reachable while signed out', async ({ page }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL(/\/login/);
  });

  /** AuthGuard carries the requested page so the user lands back where they were headed. */
  test('the requested page is carried into the login redirect', async ({ page }) => {
    await page.goto('/dashboard');

    await expect(page).toHaveURL(/returnUrl=%2Fdashboard/);
  });
});
