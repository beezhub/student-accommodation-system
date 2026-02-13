import { Injectable, NgZone } from '@angular/core';
import { PlaceDetails } from '../models/place-details.model';

export interface AutocompleteHandle {
  destroy: () => void;
  resolveAddress: () => Promise<void>;
}

@Injectable({
  providedIn: 'root'
})
export class GoogleMapsAlternativeService {
  private isLoaded = false;
  private loadPromise: Promise<void> | null = null;

  constructor(private ngZone: NgZone) {}

  loadGoogleMapsAPI(apiKey: string): Promise<void> {
    if (this.isLoaded) {
      return Promise.resolve();
    }
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = new Promise((resolve, reject) => {
      if (window.google && window.google.maps) {
        this.isLoaded = true;
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async&v=weekly`;
      script.async = true;
      script.defer = true;

      script.onload = () => {
        this.isLoaded = true;
        resolve();
      };

      script.onerror = (error) => {
        console.error('Failed to load Google Maps API:', error);
        this.loadPromise = null;
        reject(error);
      };

      document.head.appendChild(script);
    });

    return this.loadPromise;
  }

  initializeAutocomplete(
    inputElement: HTMLInputElement,
    onPlaceChanged: (place: PlaceDetails | null) => void
  ): AutocompleteHandle {

    let cleanupFn = () => {};
    let resolveFn: () => Promise<void> = () => Promise.resolve();

    (async () => {
      try {
        const libraries = await (google.maps as any).importLibrary("places") as any;
        const { PlaceAutocompleteElement, Place } = libraries;

        if (!PlaceAutocompleteElement) {
            return;
        }

        const autocompleteElement = new PlaceAutocompleteElement();
        autocompleteElement.id = 'gmp-autocomplete-input';

        // Apply CSS variables for theming (these penetrate shadow DOM)
        const cssVars = {
          '--gmpx-color-surface': '#ffffff',
          '--gmpx-color-on-surface': '#1e293b',
          '--gmpx-color-on-surface-variant': '#94a3b8',
          '--gmpx-color-primary': '#ea580c',
          '--gmpx-color-outline': '#cbd5e1',
          '--gmpx-font-family-base': "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
          '--gmpx-font-size-base': '14px',
          // Additional Material 3 tokens that might be used
          'color': '#1e293b',
          'background-color': '#ffffff',
          'font-family': "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
        };

        Object.entries(cssVars).forEach(([key, value]) => {
          autocompleteElement.style.setProperty(key, value);
        });

        // FORCE LIGHT MODE: This is critical for the "black background" issue
        // The component likely defaults to system theme (dark) if not set
        autocompleteElement.style.colorScheme = 'light';

        // Direct Shadow DOM injection to override internal styles that variables miss
        // This targets the internal structure of the PlaceAutocompleteElement
        setTimeout(() => {
          if (autocompleteElement.shadowRoot) {
            const style = document.createElement('style');
            style.textContent = `
              :host {
                background-color: #ffffff !important;
                color: #1e293b !important;
              }
              /* Target the internal input */
              input {
                color: #1e293b !important;
                background-color: transparent !important;
              }
              /* Target the dropdown container and items */
              .matches, ul, div[role="listbox"] {
                background-color: #ffffff !important;
                color: #1e293b !important;
              }
              li, div[role="option"] {
                color: #1e293b !important;
                background-color: #ffffff !important;
              }
              li:hover, div[role="option"]:hover, li[aria-selected="true"], div[aria-selected="true"] {
                background-color: #f1f5f9 !important;
              }
              /* Ensure the "powered by Google" logo area is light */
              .footer {
                background-color: #ffffff !important;
              }
            `;
            autocompleteElement.shadowRoot.appendChild(style);
          }
        }, 100);

        // Insert into DOM
        const parent = inputElement.parentElement;
        if (parent) {
            parent.insertBefore(autocompleteElement, inputElement);
            inputElement.style.display = 'none';
        }

        if (inputElement.value) {
            autocompleteElement.value = inputElement.value;
        }

        let placeSelected = false;

        // Reset placeSelected when user types a new value so resolveCurrentText can work
        autocompleteElement.addEventListener('input', () => {
          placeSelected = false;
        });

        // Helper to sync place details to the form
        const syncDetails = (details: PlaceDetails) => {
          this.ngZone.run(() => {
            onPlaceChanged(details);
            inputElement.value = details.formattedAddress;
            inputElement.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
            inputElement.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
          });
        };

        // Resolve address text via Place.searchByText
        const resolveCurrentText = async (): Promise<void> => {
          const currentText = autocompleteElement.value;
          if (placeSelected || !currentText || currentText.length < 3) {
            return;
          }

          try {
            if (Place && Place.searchByText) {
              const { places } = await Place.searchByText({
                textQuery: currentText,
                fields: ['id', 'formattedAddress', 'location', 'displayName'],
                maxResultCount: 1,
              });

              if (places && places.length > 0) {
                const found = places[0];
                placeSelected = true;
                syncDetails({
                  placeId: found.id || '',
                  formattedAddress: found.formattedAddress || currentText,
                  latitude: found.location ? found.location.lat() : 0,
                  longitude: found.location ? found.location.lng() : 0,
                });
                return;
              }
            }
          } catch (searchErr) {
            // searchByText unavailable or failed
          }

          // Last resort: sync the raw text
          this.ngZone.run(() => {
            inputElement.value = currentText;
            inputElement.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
            inputElement.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
          });
        };

        // Expose resolveAddress for the component to call before submit
        resolveFn = resolveCurrentText;

        // --- PRIMARY: gmp-placeselect event ---
        const handlePlaceSelection = async (event: any) => {
            placeSelected = true;

            const place = event.place || event.detail?.place;
            if (!place) {
                return;
            }

            try {
                 await place.fetchFields({
                    fields: ['displayName', 'formattedAddress', 'location', 'id'],
                 });

                 syncDetails({
                    placeId: place.id,
                    formattedAddress: place.formattedAddress || place.displayName || '',
                    latitude: place.location?.lat(),
                    longitude: place.location?.lng(),
                 });

            } catch (err) {
                const fallbackAddress = autocompleteElement.value || place.displayName || '';
                syncDetails({
                    placeId: place.id || '',
                    formattedAddress: fallbackAddress,
                    latitude: place.location ? place.location.lat() : 0,
                    longitude: place.location ? place.location.lng() : 0,
                });
            }
        };

        autocompleteElement.addEventListener('gmp-placeselect', handlePlaceSelection);

        cleanupFn = () => {
          autocompleteElement.removeEventListener('gmp-placeselect', handlePlaceSelection);
          if (parent && parent.contains(autocompleteElement)) {
            parent.removeChild(autocompleteElement);
          }
          inputElement.style.display = '';
        };

      } catch (e) {
        // PlaceAutocompleteElement initialization failed
      }
    })();

    return {
      destroy: () => cleanupFn(),
      resolveAddress: () => resolveFn(),
    };
  }
}
