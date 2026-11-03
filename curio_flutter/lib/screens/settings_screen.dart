import 'package:flutter/material.dart';

import '../data/lanes.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_widgets.dart';

/// Settings, as three short sections rather than the phone's long hub: a desktop
/// window can show all of a small app's settings at once, and a shell that is
/// four rooms wide has no room to hide an option two taps down.
class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key, required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return PageBody(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeader(
            title: 'Settings',
            subtitle: 'The few things this shell keeps for you.',
          ),
          const SizedBox(height: CurioSpace.gap + 4),
          _Section(
            title: 'Appearance',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _RowLabel(
                  title: 'Theme',
                  note: 'Follow Windows, or pin light or dark.',
                ),
                const SizedBox(height: 10),
                SegmentedButton<ThemeMode>(
                  segments: const [
                    ButtonSegment(
                      value: ThemeMode.system,
                      label: Text('System'),
                      icon: Icon(Icons.contrast_rounded, size: 16),
                    ),
                    ButtonSegment(
                      value: ThemeMode.light,
                      label: Text('Light'),
                      icon: Icon(Icons.light_mode_outlined, size: 16),
                    ),
                    ButtonSegment(
                      value: ThemeMode.dark,
                      label: Text('Dark'),
                      icon: Icon(Icons.dark_mode_outlined, size: 16),
                    ),
                  ],
                  selected: <ThemeMode>{model.themeMode},
                  onSelectionChanged: (selection) =>
                      model.setTheme(selection.first),
                ),
              ],
            ),
          ),
          const SizedBox(height: CurioSpace.gap),
          _Section(
            title: 'Storage',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _RowLabel(
                  title: 'This device',
                  note: 'The cabinet and the chosen lane are two JSON files here.',
                ),
                const SizedBox(height: 8),
                SelectableText(
                  model.dataPath,
                  style: TextStyle(
                    fontSize: 12,
                    height: 1.5,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        '${model.saved.length} kept · ${model.totalTopics} topics '
                        'in ${kLanes.length} lanes',
                        style: const TextStyle(fontSize: 12.5),
                      ),
                    ),
                    OutlinedButton.icon(
                      onPressed: model.saved.isEmpty ? null : model.clearCabinet,
                      icon: const Icon(Icons.delete_sweep_outlined, size: 18),
                      label: const Text('Clear the Cabinet'),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: CurioSpace.gap),
          _Section(
            title: 'About',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _RowLabel(
                  title: 'Curio',
                  note: 'One good thing to explore, and somewhere to keep it.',
                ),
                const SizedBox(height: 8),
                Text(
                  'Windows build 1.0.0 · catalog and lane colours read from the '
                  'Android app\'s own declarations, so the two never disagree.',
                  style: TextStyle(
                    fontSize: 12,
                    height: 1.5,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title.toUpperCase(),
            style: TextStyle(
              fontSize: 10.5,
              fontWeight: FontWeight.w800,
              letterSpacing: 1.1,
              color: scheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 12),
          child,
        ],
      ),
    );
  }
}

class _RowLabel extends StatelessWidget {
  const _RowLabel({required this.title, required this.note});

  final String title;
  final String note;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
        const SizedBox(height: 2),
        Text(
          note,
          style: TextStyle(fontSize: 12, height: 1.45, color: scheme.onSurfaceVariant),
        ),
      ],
    );
  }
}
