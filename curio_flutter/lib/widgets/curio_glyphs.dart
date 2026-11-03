import 'package:flutter/material.dart';

/// Curio's own glyphs, drawn rather than borrowed.
///
/// The Android app ships a single icon set (`CurioIcons` + `CurioIcon`) so a
/// lane, a room and a tool all speak one hand: rounded, a little chunky, drawn
/// at a 24-unit grid with a 1.6 stroke. Material's stock glyphs are close in
/// some places and clearly from another family in others (its die, its bookmark,
/// its tune sliders), and a window that mixes the two reads as a Flutter demo
/// rather than as Curio.
///
/// So this is the same idea on this side: the shapes below are drawn in the
/// phone's 24-unit box, in the phone's stroke, and every one of them is a
/// `CustomPaint` — no font, no asset, no package. `CurioGlyphIcon` is the only
/// way they are used, so a glyph can never be scaled or weighted by accident.
enum CurioGlyph {
  /// The room mark: a roof over a sheet.
  home,

  /// The die the deck is dealt with.
  spin,

  /// A drawer of the Cabinet.
  cabinet,

  /// Three sliders — the phone's `Tune` glyph.
  settings,

  /// The four-point star the app uses for a kept, a landed, a saved thing.
  spark,

  /// The lane compass.
  compass,

  /// A quill — the writing desk.
  feather,

  /// An open book.
  book,

  /// A sealed letter (a time capsule).
  letter,
}

class CurioGlyphIcon extends StatelessWidget {
  const CurioGlyphIcon(
    this.glyph, {
    super.key,
    this.size = 20,
    this.color,
    this.stroke = 1.7,
  });

  final CurioGlyph glyph;
  final double size;

  /// Defaults to the `IconTheme` the glyph sits in, so it behaves like an icon —
  /// a rail button's ink, a lit tool's knocked-out colour.
  final Color? color;
  final double stroke;

  @override
  Widget build(BuildContext context) {
    final ink = color ??
        IconTheme.of(context).color ??
        Theme.of(context).colorScheme.onSurfaceVariant;
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(
        painter: _GlyphPainter(glyph: glyph, ink: ink, stroke: stroke),
      ),
    );
  }
}

class _GlyphPainter extends CustomPainter {
  const _GlyphPainter({
    required this.glyph,
    required this.ink,
    required this.stroke,
  });

  final CurioGlyph glyph;
  final Color ink;
  final double stroke;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.save();
    canvas.scale(size.width / 24, size.height / 24);
    final pen = Paint()
      ..color = ink
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..isAntiAlias = true;
    final fill = Paint()
      ..color = ink
      ..style = PaintingStyle.fill
      ..isAntiAlias = true;

    switch (glyph) {
      case CurioGlyph.home:
        canvas.drawPath(
          Path()
            ..moveTo(3.6, 11.2)
            ..lineTo(12, 4)
            ..lineTo(20.4, 11.2),
          pen,
        );
        canvas.drawRRect(
          RRect.fromRectAndCorners(
            const Rect.fromLTRB(6, 10.6, 18, 20),
            bottomLeft: const Radius.circular(3),
            bottomRight: const Radius.circular(3),
          ),
          pen,
        );

      case CurioGlyph.spin:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTRB(4, 4, 20, 20),
            const Radius.circular(4.6),
          ),
          pen,
        );
        for (final pip in const <Offset>[
          Offset(9.2, 9.2),
          Offset(12, 12),
          Offset(14.8, 14.8),
        ]) {
          canvas.drawCircle(pip, 1.45, fill);
        }

      case CurioGlyph.cabinet:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTRB(4.6, 4, 19.4, 20),
            const Radius.circular(3),
          ),
          pen,
        );
        canvas.drawLine(const Offset(6.4, 12.4), const Offset(17.6, 12.4), pen);
        canvas.drawLine(const Offset(10.6, 8.2), const Offset(13.4, 8.2), pen);
        canvas.drawLine(const Offset(10.6, 16.6), const Offset(13.4, 16.6), pen);

      case CurioGlyph.settings:
        for (final row in const <double>[7, 12, 17]) {
          canvas.drawLine(Offset(3.8, row), Offset(20.2, row), pen);
        }
        for (final knob in const <Offset>[
          Offset(9.4, 7),
          Offset(15.2, 12),
          Offset(8.4, 17),
        ]) {
          canvas.drawCircle(knob, 2.2, fill);
        }

      case CurioGlyph.spark:
        canvas.drawPath(
          Path()
            ..moveTo(12, 2.6)
            ..quadraticBezierTo(13.6, 10.4, 21.4, 12)
            ..quadraticBezierTo(13.6, 13.6, 12, 21.4)
            ..quadraticBezierTo(10.4, 13.6, 2.6, 12)
            ..quadraticBezierTo(10.4, 10.4, 12, 2.6)
            ..close(),
          fill,
        );

      case CurioGlyph.compass:
        canvas.drawCircle(const Offset(12, 12), 8.2, pen);
        canvas.drawPath(
          Path()
            ..moveTo(12, 5.6)
            ..lineTo(15.1, 15.4)
            ..lineTo(12, 13.5)
            ..lineTo(8.9, 15.4)
            ..close(),
          fill,
        );

      case CurioGlyph.feather:
        canvas.drawPath(
          Path()
            ..moveTo(19, 4.4)
            ..quadraticBezierTo(9.6, 5.8, 6.4, 15.2)
            ..quadraticBezierTo(5.6, 17.8, 4.6, 19.4)
            ..quadraticBezierTo(11.6, 19, 15.8, 13.4)
            ..quadraticBezierTo(18.4, 10, 19, 4.4)
            ..close(),
          pen,
        );
        canvas.drawLine(const Offset(4.8, 19.2), const Offset(14.4, 9.6), pen);

      case CurioGlyph.book:
        final left = Path()
          ..moveTo(12, 7.6)
          ..cubicTo(9.4, 6.2, 6.2, 6.2, 3.8, 7.6)
          ..lineTo(3.8, 17.6)
          ..cubicTo(6.2, 16.2, 9.4, 16.2, 12, 17.6);
        final right = Path()
          ..moveTo(12, 7.6)
          ..cubicTo(14.6, 6.2, 17.8, 6.2, 20.2, 7.6)
          ..lineTo(20.2, 17.6)
          ..cubicTo(17.8, 16.2, 14.6, 16.2, 12, 17.6);
        canvas.drawPath(left, pen);
        canvas.drawPath(right, pen);

      case CurioGlyph.letter:
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTRB(3, 6, 21, 18),
            const Radius.circular(2.6),
          ),
          pen,
        );
        canvas.drawPath(
          Path()
            ..moveTo(3.8, 7.4)
            ..lineTo(12, 13)
            ..lineTo(20.2, 7.4),
          pen,
        );
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(_GlyphPainter old) =>
      old.glyph != glyph || old.ink != ink || old.stroke != stroke;
}
