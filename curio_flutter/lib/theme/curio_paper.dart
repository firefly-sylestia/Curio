import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

/// The paper under every room.
///
/// This is the part of the phone's look that is NOT the torn edge: the sheet
/// itself. On Android every paper surface is a `PaperCard` whose texture is
/// three layers — a soft TONAL CLOUD field (four big radial patches, alternately
/// darkened and whitened), a fine SPECK grain (a seeded scatter of sub-pixel
/// dots), and the sheet's own wash (`PaperTextureSpec.build`). Nothing is
/// random per frame: the geometry is a pure function of the size and a seed, so
/// the same window is the same paper each time it is drawn.
///
/// The shell paints one of these behind the rooms, and the journal page paints
/// one inside its sheet, which is what makes a Curio window read as a table with
/// paper on it rather than as a flat dark rectangle. It is a `CustomPaint` over
/// pre-built geometry — a redraw replays a few dozen primitives and allocates
/// nothing, so it costs less than a single gradient card.
class PaperBackdrop extends StatelessWidget {
  const PaperBackdrop({
    super.key,
    this.seed = 7,
    this.strength = 1,
    this.clouds = true,
  });

  /// Stable per surface, so a room's paper never re-scatters when it repaints.
  final int seed;

  /// 1 for a page, less for a shell behind cards (the pattern is under glass).
  final double strength;

  /// The tonal cloud field. The journal page keeps it; the shell may drop it,
  /// since the clouds are large and a window is already lit unevenly.
  final bool clouds;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final ink = dark ? Colors.white : CurioPaperInk.ink;
    return LayoutBuilder(
      builder: (context, constraints) {
        return CustomPaint(
          size: Size(constraints.maxWidth, constraints.maxHeight),
          painter: _PaperPainter(
            seed: seed,
            ink: ink,
            dark: dark,
            strength: strength,
            clouds: clouds,
          ),
        );
      },
    );
  }
}

/// The ink the specks and clouds are mixed from, in one place.
abstract final class CurioPaperInk {
  /// The phone's page ink (`CurioColors.Ink`), at the alpha the texture uses.
  static const ink = Color(0xFF241C1B);
}

class _PaperPainter extends CustomPainter {
  _PaperPainter({
    required this.seed,
    required this.ink,
    required this.dark,
    required this.strength,
    required this.clouds,
  });

  final int seed;
  final Color ink;
  final bool dark;
  final double strength;
  final bool clouds;

  // The built geometry, kept across frames: the paper only changes when the
  // window is resized (or the theme flips), never while typing or scrolling.
  Size? _built;
  List<_Cloud> _patches = const <_Cloud>[];
  List<_Speck> _specks = const <_Speck>[];
  List<_Fibre> _fibres = const <_Fibre>[];

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) return;
    if (_built != size) {
      _build(size);
      _built = size;
    }

    // Layer 1 — the tonal clouds: four big, very faint radial patches, half of
    // them the page ink and half paper-white, which is what makes the sheet read
    // as unevenly lit rather than as a tint.
    if (clouds) {
      for (final patch in _patches) {
        final paint = Paint()
          ..shader = ui.Gradient.radial(patch.centre, patch.radius, <Color>[
            patch.colour,
            patch.colour.withValues(alpha: 0),
          ]);
        canvas.drawCircle(patch.centre, patch.radius, paint);
      }
    }

    // Layer 2 — the speck grain, the paper's tooth.
    final speckPaint = Paint()..isAntiAlias = true;
    for (final speck in _specks) {
      speckPaint.color = speck.colour;
      canvas.drawCircle(speck.centre, speck.radius, speckPaint);
    }

    // Layer 3 — a whisper of laid fibre (the faint horizontal lines a laid sheet
    // has). It is what stops the grain looking like dust on a screen.
    final fibrePaint = Paint()
      ..strokeWidth = 0.7
      ..strokeCap = StrokeCap.round;
    for (final fibre in _fibres) {
      fibrePaint.color = fibre.colour;
      canvas.drawLine(fibre.from, fibre.to, fibrePaint);
    }
  }

  void _build(Size size) {
    final w = size.width;
    final h = size.height;
    final random = math.Random(seed);

    // The phone's four clouds, same construction (alternating ink and white at
    // 1.5–3.5% and a radius of 25–70% of the shorter side).
    final patches = <_Cloud>[];
    for (var index = 0; index < 4; index++) {
      final cx = w * (0.1 + random.nextDouble() * 0.8);
      final cy = h * (0.1 + random.nextDouble() * 0.8);
      final radius = math.min(w, h) * (0.25 + random.nextDouble() * 0.45);
      final alpha = (0.015 + random.nextDouble() * 0.020) * strength;
      final colour = index.isEven
          ? ink.withValues(alpha: dark ? alpha * 0.7 : alpha)
          : Colors.white.withValues(alpha: alpha);
      patches.add(_Cloud(Offset(cx, cy), radius, colour));
    }

    // One speck per ~80dp² (the phone's density), floored and capped so a huge
    // window does not pay for a grain field it cannot see.
    final count = (w * h / (80 * 80)).toInt().clamp(60, 300);
    final grain = math.Random(seed + 0x1F3D5);
    final specks = <_Speck>[];
    for (var index = 0; index < count; index++) {
      final sx = grain.nextDouble() * w;
      final sy = grain.nextDouble() * h;
      final radius = 0.4 + grain.nextDouble() * 0.9;
      final alpha = (0.03 + grain.nextDouble() * 0.05) * 0.30 * strength;
      specks.add(
        _Speck(Offset(sx, sy), radius, ink.withValues(alpha: dark ? alpha * 1.6 : alpha)),
      );
    }

    final laid = math.Random(seed + 0x5EED);
    final fibres = <_Fibre>[];
    final rows = (h / 26).ceil();
    for (var row = 0; row < rows; row++) {
      final y = row * 26.0 + laid.nextDouble() * 6;
      final x = laid.nextDouble() * w * 0.5;
      final length = 30 + laid.nextDouble() * 90;
      fibres.add(
        _Fibre(
          Offset(x, y),
          Offset(math.min(x + length, w), y),
          ink.withValues(alpha: 0.012 * strength),
        ),
      );
    }

    _patches = patches;
    _specks = specks;
    _fibres = fibres;
  }

  @override
  bool shouldRepaint(_PaperPainter old) =>
      old.seed != seed ||
      old.ink != ink ||
      old.dark != dark ||
      old.strength != strength ||
      old.clouds != clouds;
}

class _Cloud {
  const _Cloud(this.centre, this.radius, this.colour);

  final Offset centre;
  final double radius;
  final Color colour;
}

class _Speck {
  const _Speck(this.centre, this.radius, this.colour);

  final Offset centre;
  final double radius;
  final Color colour;
}

class _Fibre {
  const _Fibre(this.from, this.to, this.colour);

  final Offset from;
  final Offset to;
  final Color colour;
}
