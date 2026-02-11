# Google Maps Autocomplete Verification (Final Debug)

We have added support for **Legacy Events** and **Error Trapping** to catch why the selection isn't registering.

## Verification Steps

1.  **Open Console (F12)** -> Warnings Enabled.
2.  **Add Building** -> Select Address.

## What to look for

*   **API Capabilities:**
    *   `✅ Available Classes: ...` -> Does it list `PlaceAutocompleteElement`?

*   **Event Firing:**
    *   `📍 gmp-placeselect FIRED` (Modern)
    *   **OR** `📍 place_changed FIRED` (Legacy fallback)

*   **Errors:**
    *   `🚨 Google Maps Internal Error:` -> This would confirm an API Key/Billing issue preventing the data fetch.

## Final Diagnosis Logic

*   If `gmp-placeselect` NEVER fires...
*   AND you see `click` events on the element...
*   **THEN: The Google Cloud "Places API (New)" is NOT enabled.**
    *   The component allows typing (prediction) but fails to "select" (get details) because the `GetPlace` (New) endpoint is blocked.
    *   **Solution:** Enable **"Places API (New)"** in Google Cloud Console.
