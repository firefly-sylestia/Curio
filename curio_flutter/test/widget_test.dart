import 'package:curio/theme/curio_theme.dart';
import 'package:curio/widgets/curio_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// A tiny widget smoke test.
///
/// It exists for two reasons: the shared page chrome is what every room wears,
/// so a broken header is a broken app; and this filename is the one
/// `flutter create` writes for a new project — having our own means the CI's
/// generated runner step can never drop a `MyApp` template test into the tree
/// that would fail to compile against this app.
void main() {
  Widget host(Widget child) => MaterialApp(
        theme: curioTheme(Brightness.light),
        home: Scaffold(body: child),
      );

  testWidgets('the page header shows its title and subtitle', (tester) async {
    await tester.pumpWidget(
      host(
        const PageHeader(
          title: 'Curio',
          subtitle: 'One good thing to explore.',
        ),
      ),
    );
    expect(find.text('Curio'), findsOneWidget);
    expect(find.text('One good thing to explore.'), findsOneWidget);
  });

  testWidgets('a lane chip reads on its own accent when selected',
      (tester) async {
    await tester.pumpWidget(
      host(
        Center(
          child: LaneChip(
            label: 'Books',
            accent: 0xFFB45309,
            selected: true,
            onTap: () {},
          ),
        ),
      ),
    );
    final text = tester.widget<Text>(find.text('Books'));
    // #B45309 is a deep accent, so the chip's word has to be the paper white —
    // the same rule the phone's `settingsReadableInk` applies.
    expect(text.style?.color, inkOn(const Color(0xFFB45309)));
  });
}
