import 'package:flutter/material.dart';

import 'shell/app_shell.dart';
import 'store/app_model.dart';
import 'theme/curio_theme.dart';

/// Curio for Windows (and, next, iOS).
///
/// This is a separate app from the Android one, in a separate language: it reads
/// the SAME topic catalog (`data/topics/*.json`, bundled by
/// `tool/sync_assets.sh`) and the same lane declarations (generated into
/// `lib/data/lanes.dart` from the Kotlin catalog), and it keeps its own cabinet
/// in its own folder. Nothing here shares code with `app/` on purpose — the
/// window wants a different layout and its own way of holding state.
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(CurioApp(model: AppModel()));
}

class CurioApp extends StatefulWidget {
  const CurioApp({super.key, required this.model});

  final AppModel model;

  @override
  State<CurioApp> createState() => _CurioAppState();
}

class _CurioAppState extends State<CurioApp> {
  @override
  void initState() {
    super.initState();
    // Reads the catalog and both JSON stores. The shell shows its spinner while
    // this runs and reports a failure on the page rather than opening blank.
    widget.model.boot();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.model,
      builder: (context, _) => MaterialApp(
        title: 'Curio',
        debugShowCheckedModeBanner: false,
        theme: curioTheme(Brightness.light),
        darkTheme: curioTheme(Brightness.dark),
        themeMode: widget.model.themeMode,
        home: CurioShell(model: widget.model),
      ),
    );
  }
}
