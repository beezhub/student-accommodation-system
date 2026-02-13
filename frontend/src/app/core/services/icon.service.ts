import { Injectable } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

@Injectable({
  providedIn: 'root',
})
export class IconService {
  private registeredIcons = new Set<string>();

  constructor(private matIconRegistry: MatIconRegistry, private domSanitizer: DomSanitizer) {
    this.registerCommonIcons();
  }

  /**
   * Register common icons used throughout the application
   */
  private registerCommonIcons(): void {
    // Application specific icons
    const commonIcons = [
      'home',
      'dashboard',
      'assignment',
      'folder',
      'person',
      'logout',
      'login',
      'person_add',
      'upload_file',
      'visibility',
      'edit',
      'delete',
      'download',
      'close',
      'add',
      'flash_on',
      'support_agent',
      'description',
      'security',
      'check_circle',
      'arrow_forward',
      'arrow_back',
      'expand_more',
      'menu',
      'account_circle',
      'lock',
      'history',
      'folder_open',
      'attach_file',
      'visibility_off',
      'upload',
    ];

    commonIcons.forEach((icon) => {
      if (!this.registeredIcons.has(icon)) {
        this.registeredIcons.add(icon);
      }
    });
  }

  /**
   * Register a custom SVG icon from a URL
   */
  registerSvgIcon(iconName: string, url: string): void {
    if (!this.registeredIcons.has(iconName)) {
      this.matIconRegistry.addSvgIcon(
        iconName,
        this.domSanitizer.bypassSecurityTrustResourceUrl(url)
      );
      this.registeredIcons.add(iconName);
    }
  }

  /**
   * Register a custom SVG icon from inline SVG content
   */
  registerSvgIconLiteral(iconName: string, svgContent: string): void {
    if (!this.registeredIcons.has(iconName)) {
      this.matIconRegistry.addSvgIconLiteral(
        iconName,
        this.domSanitizer.bypassSecurityTrustHtml(svgContent)
      );
      this.registeredIcons.add(iconName);
    }
  }

  /**
   * Register an icon set from a URL
   */
  registerIconSet(url: string, namespace?: string): void {
    if (namespace) {
      this.matIconRegistry.addSvgIconSetInNamespace(
        namespace,
        this.domSanitizer.bypassSecurityTrustResourceUrl(url)
      );
    } else {
      this.matIconRegistry.addSvgIconSet(this.domSanitizer.bypassSecurityTrustResourceUrl(url));
    }
  }

  /**
   * Check if an icon is registered
   */
  isIconRegistered(iconName: string): boolean {
    return this.registeredIcons.has(iconName);
  }

  /**
   * Get all registered icons
   */
  getRegisteredIcons(): string[] {
    return Array.from(this.registeredIcons);
  }
}
