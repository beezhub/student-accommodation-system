/**
 * How to reach support for this deployment, from GET /api/contact. The backend reads
 * APP_SUPPORT_EMAIL and APP_CONTACT_PHONE, the same values the email footer shows.
 */
export interface ContactInfo {
  email: string;
  /** Null when the deployment has no phone number configured. */
  phone: string | null;
}

/**
 * A dialable tel: link. Drops the "(0)" written after a country code, since +27 0 11 does not
 * connect, then keeps only digits and +.
 */
export function telHref(phone: string): string {
  return `tel:${phone.replace(/\(0\)/g, '').replace(/[^\d+]/g, '')}`;
}
