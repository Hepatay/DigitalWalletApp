# Bottom Navigation Bar Standardization

This plan addresses the issue where the bottom navigation bar and footer area appear inconsistently sized across different devices (3-button vs. gesture navigation).

## User Review Required

> [!IMPORTANT]
> The total height of the bottom area will be reduced to provide more space for main content. The "Legal" and "Privacy" links will be more compact but will still meet accessibility guidelines for touch targets.

## Proposed Changes

### [UI Layout]

#### [MODIFY] [activity_main.xml](file:///C:/digitalwallet/app/src/main/res/layout/activity_main.xml)
- Optimize the `bottomNavigationContainer` to be more compact.
- Reduce `BottomNavigationView` height from 64dp to 56dp (standard for labeled icons).
- Revise `legalFooter` height from 40dp to `wrap_content`.
- Adjust `tvLegalLink`, `tvInvestmentDisclaimer`, and `tvPrivacyLink` to have a minimum touch target height of 40dp but less vertical padding to save space.
- Remove hardcoded 48dp heights that conflict with parent constraints.

### [Logic]

#### [MODIFY] [MainActivity.kt](file:///C:/digitalwallet/app/src/main/java/com/epatay/digitalwallet/MainActivity.kt)
- Update `OnApplyWindowInsetsListener` to handle bottom insets more cleanly.
- Remove the arbitrary `+ 1dp` padding.
- Ensure the background of the bottom container properly extends into the navigation bar area on gesture-based devices without adding excessive "dead space" on 3-button devices.

## Verification Plan

### Manual Verification
- Deploy to an emulator/device with **3-button navigation** and verify the bottom bar doesn't look excessively tall.
- Deploy to an emulator/device with **Gesture navigation** and verify the bottom bar has proper breathing room and isn't too cramped or floating too high.
- Verify that footer links remain clickable and legible.
