import { Injectable } from '@angular/core';
import { PlaceDetails } from '../models/place-details.model';

@Injectable({
  providedIn: 'root'
})
export class GoogleMapsService {
  private isLoaded = false;
  private loadPromise: Promise<void> | null = null;

  /**
   * Dynamically loads the Google Maps JavaScript API
   */
  loadGoogleMapsAPI(apiKey: string): Promise<void> {
    if (this.isLoaded) {
      return Promise.resolve();
    }

    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places`;
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

  /**
   * Initializes autocomplete on an input element using the NEW PlaceAutocompleteElement API
   * (Required for API keys created after March 1, 2025)
   */
  initializeAutocomplete(
    inputElement: HTMLInputElement,
    onPlaceChanged: (place: PlaceDetails | null) => void
  ): google.maps.places.PlaceAutocompleteElement | null {
    if (!this.isLoaded || !window.google || !window.google.maps) {
      console.error('Google Maps API not loaded - cannot initialize autocomplete');
      return null;
    }

    try {
      // Create the new PlaceAutocompleteElement
      const autocompleteElement = new google.maps.places.PlaceAutocompleteElement({
        componentRestrictions: { country: [] },
      });

      autocompleteElement.id = 'place-autocomplete';

      // Replace the input with autocomplete element
      inputElement.parentElement?.insertBefore(autocompleteElement, inputElement);
      inputElement.style.display = 'none';

      // Get the inner input from the autocomplete element to sync values
      const autocompleteInput = autocompleteElement.querySelector('input');

      // Sync autocomplete input with the original hidden input (for form binding)
      if (autocompleteInput) {
        // When user types in autocomplete, update the hidden input
        autocompleteInput.addEventListener('input', (e: any) => {
          inputElement.value = e.target.value;
          inputElement.dispatchEvent(new Event('input', { bubbles: true }));
        });

        // If the form value changes programmatically, update autocomplete
        const observer = new MutationObserver(() => {
          if (inputElement.value && autocompleteInput.value !== inputElement.value) {
            autocompleteInput.value = inputElement.value;
          }
        });
        observer.observe(inputElement, { attributes: true, attributeFilter: ['value'] });
      }

      // Debug: Log when autocomplete element is created
      console.log('Autocomplete element created:', autocompleteElement);

      // Define the event handler
      const handlePlaceSelect = async (event: any) => {
        console.log('🎯 Place select event fired!', event);

        try {
          const place = event.place;
          console.log('Place object:', place);

          if (place) {
            // Fetch full place details
            console.log('Fetching place details...');
            await place.fetchFields({
              fields: ['displayName', 'formattedAddress', 'location', 'addressComponents']
            });

            console.log('Place details fetched:', place);

            const placeDetails: PlaceDetails = {
              placeId: place.id || '',
              formattedAddress: place.formattedAddress || place.displayName || '',
              latitude: place.location?.lat() || 0,
              longitude: place.location?.lng() || 0
            };

            console.log('✅ Place selected:', placeDetails);

            // IMPORTANT: Call the callback FIRST to update the form
            onPlaceChanged(placeDetails);

            // Then update the inputs to reflect the selection
            inputElement.value = placeDetails.formattedAddress;
            if (autocompleteInput) {
              autocompleteInput.value = placeDetails.formattedAddress;
            }

            // Trigger change events for Angular form to detect
            const inputEvent = new Event('input', { bubbles: true });
            const changeEvent = new Event('change', { bubbles: true });
            inputElement.dispatchEvent(inputEvent);
            inputElement.dispatchEvent(changeEvent);

            console.log('Input value updated to:', inputElement.value);
          } else {
            console.warn('No place object in event');
          }
        } catch (error) {
          console.error('Error handling place selection:', error);
        }
      };

      // Wait for the element to be fully initialized before attaching listeners
      // Use requestAnimationFrame to ensure DOM is ready
      requestAnimationFrame(() => {
        console.log('Attaching event listeners after element initialization...');

        // Try multiple event names
        autocompleteElement.addEventListener('gmp-placeselect', handlePlaceSelect);
        autocompleteElement.addEventListener('place_changed', handlePlaceSelect);

        // Also listen on the document in case the event bubbles
        document.addEventListener('gmp-placeselect', (e: any) => {
          if (e.target === autocompleteElement) {
            console.log('Event caught on document level');
            handlePlaceSelect(e);
          }
        });

        console.log('✅ Event listeners attached to autocomplete element');
      });

      // Also try using the element's property directly if available
      (autocompleteElement as any).addEventListener?.('gmp-placeselect', (e: any) => {
        console.log('Direct addEventListener fired');
        handlePlaceSelect(e);
      });

      return autocompleteElement;
    } catch (error) {
      console.error('Error creating PlaceAutocompleteElement:', error);
      return null;
    }
  }

  /**
   * Extracts place details from Google Places result
   */
  extractPlaceDetails(place: google.maps.places.PlaceResult): PlaceDetails | null {
    if (!place || !place.place_id || !place.geometry || !place.geometry.location) {
      console.warn('Invalid place result:', place);
      return null;
    }

    return {
      placeId: place.place_id,
      formattedAddress: place.formatted_address || '',
      latitude: place.geometry.location.lat(),
      longitude: place.geometry.location.lng()
    };
  }
}
