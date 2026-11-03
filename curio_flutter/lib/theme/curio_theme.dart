import 'package:flutter/material.dart';

/// Curio's palette, mirrored from the Android app's `CurioColors` so the phone
/// and this shell read as one product. Every value here has a Kotlin twin — if a
/// colour moves there, it moves here.
abstract final class CurioPalette {
  /// The brand rose. `HomeRosewood` on the phone, and the same on every hero.
  static const rose = Color(0xFFDC7482);

  /// The dark-mode twin of [rose] — the same hue, muted for a night page
  /// (the phone's v421 "in dark mode the curio rose is too vibrant").
  static const roseNight = Color(0xFFA25B66);

  static const coralBlush = Color(0xFFFF8FA3);
  static const coralInk = Color(0xFFE2556B);

  /// The paper family: warm cream, never white, so long reading is easier.
  static const cream = Color(0xFFF7F0E4);
  static const creamWhite = Color(0xFFFFFBF5);
  static const sand = Color(0xFFF6EFE4);

  static const ink = Color(0xFF241C1B);
  static const plum = Color(0xFF3B0A17);

  /// The night page and the raised surface on it.
  static const night = Color(0xFF14100F);
  static const nightRaised = Color(0xFF1E1917);
  static const nightInk = Color(0xFFF3E9E4);
}

/// Shape and spacing tokens. The desktop shell breathes more than the phone
/// (a 28dp page padding is right on a 1440px window and wrong on a 360dp one),
/// so layout numbers live here rather than being sprinkled through widgets.
abstract final class CurioSpace {
  static const double railWidth = 76;
  static const double paneWidth = 300;
  static const double page = 28;
  static const double gap = 16;
  static const double cardRadius = 20;
  static const double pageRadius = 28;
}

/// The light or dark theme for [brightness].
ThemeData curioTheme(Brightness brightness) {
  final dark = brightness == Brightness.dark;
  final scheme = ColorScheme.fromSeed(
    seedColor: CurioPalette.rose,
    brightness: brightness,
  ).copyWith(
    primary: dark ? CurioPalette.rose : CurioPalette.coralInk,
    onPrimary: Colors.white,
    secondary: dark ? CurioPalette.coralBlush : CurioPalette.rose,
    surface: dark ? CurioPalette.night : CurioPalette.cream,
    onSurface: dark ? CurioPalette.nightInk : CurioPalette.ink,
    surfaceContainerHighest:
        dark ? const Color(0xFF2A2321) : CurioPalette.sand,
  );

  final base = ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: scheme,
    scaffoldBackgroundColor: scheme.surface,
    splashFactory: InkSparkle.splashFactory,
  );

  return base.copyWith(
    textTheme: base.textTheme.copyWith(
      displaySmall: base.textTheme.displaySmall?.copyWith(
        fontWeight: FontWeight.w800,
        letterSpacing: -0.5,
      ),
      headlineSmall: base.textTheme.headlineSmall?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: -0.2,
      ),
      titleMedium: base.textTheme.titleMedium?.copyWith(
        fontWeight: FontWeight.w600,
      ),
      bodyMedium: base.textTheme.bodyMedium?.copyWith(height: 1.45),
    ),
    dividerTheme: DividerThemeData(
      color: scheme.outlineVariant.withValues(alpha: 0.5),
      thickness: 1,
      space: 1,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(0, 46),
        padding: const EdgeInsets.symmetric(horizontal: 22),
        shape: const StadiumBorder(),
        textStyle: const TextStyle(fontWeight: FontWeight.w600),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(shape: const StadiumBorder()),
    ),
    tooltipTheme: TooltipThemeData(
      waitDuration: const Duration(milliseconds: 450),
      textStyle: base.textTheme.bodySmall,
    ),
  );
}

/// A lane's card wash: its own accent, lightened at the top of the card and
/// deepened at the foot, so a stack of lanes reads as tinted paper rather than
/// as flat swatches. The same construction the phone's lane chips use.
LinearGradient laneGradient(int accent, {bool dark = false}) {
  final base = Color(accent);
  final top = Color.lerp(base, Colors.white, dark ? 0.04 : 0.22)!;
  final bottom = Color.lerp(base, Colors.black, dark ? 0.18 : 0.08)!;
  return LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [top, bottom],
  );
}

/// Ink that reads on [fill] (the phone's `settingsReadableInk`): a light fill
/// takes a deep ink, a deep fill keeps the paper white.
Color inkOn(Color fill) {
  final luminance = fill.computeLuminance();
  return luminance > 0.55 ? CurioPalette.plum : Colors.white;
}
