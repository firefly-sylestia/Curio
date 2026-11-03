import 'package:flutter/material.dart';

import '../data/lanes.dart';
import '../data/topic.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_widgets.dart';

/// The Cabinet: everything kept, as a grid of tinted cards.
///
/// The grid's column count comes from the window's width (a 1440px window fits
/// four, a small one fits two) — the same `maxCrossAxisExtent` idea the phone
/// uses for its shelf, which is what keeps a wide window from stretching each
/// card into a banner.
class CabinetScreen extends StatelessWidget {
  const CabinetScreen({super.key, required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return PageBody(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: 'Cabinet',
            subtitle: model.saved.isEmpty
                ? 'Nothing kept yet — a card you spin can be kept here.'
                : '${model.saved.length} ${model.saved.length == 1 ? 'discovery' : 'discoveries'} kept',
            trailing: model.saved.isEmpty
                ? null
                : TextButton.icon(
                    onPressed: () => _confirmClear(context),
                    icon: const Icon(Icons.delete_sweep_outlined, size: 18),
                    label: const Text('Clear'),
                  ),
          ),
          const SizedBox(height: CurioSpace.gap + 4),
          if (model.saved.isEmpty)
            _EmptyCabinet(hasCatalog: !model.catalogEmpty)
          else
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                maxCrossAxisExtent: 300,
                mainAxisExtent: 190,
                crossAxisSpacing: 14,
                mainAxisSpacing: 14,
              ),
              itemCount: model.saved.length,
              itemBuilder: (context, index) {
                final entry = model.saved[index];
                return _SavedCard(
                  topic: entry.topic,
                  savedAt: entry.savedAt,
                  onRemove: () => model.remove(entry.topic.id),
                );
              },
            ),
          if (model.saved.isNotEmpty) ...[
            const SizedBox(height: 18),
            Text(
              'Kept on this device. The phone\'s own Cabinet is separate until a '
              'sync exists.',
              style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _confirmClear(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear the Cabinet?'),
        content: const Text(
          'Every kept discovery is removed. This cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Keep them'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Clear'),
          ),
        ],
      ),
    );
    if (confirmed ?? false) await model.clearCabinet();
  }
}

class _SavedCard extends StatelessWidget {
  const _SavedCard({
    required this.topic,
    required this.savedAt,
    required this.onRemove,
  });

  final Topic topic;
  final DateTime savedAt;
  final VoidCallback onRemove;

  static String _date(DateTime value) {
    final local = value.toLocal();
    const months = <String>[
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
    ];
    return '${local.day} ${months[local.month - 1]} ${local.year}';
  }

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final lane = laneById(topic.categoryId.toLowerCase().replaceAll('_', '-'));
    final ink = inkOn(Color(lane.accent));
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
        onTap: () => _showDetail(context, topic),
        child: Ink(
          decoration: BoxDecoration(
            gradient: laneGradient(lane.accent, dark: dark),
            borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
          ),
          child: Stack(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 16, 18, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      lane.label.toUpperCase(),
                      style: TextStyle(
                        fontSize: 9.5,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1.1,
                        color: ink.withValues(alpha: 0.75),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Expanded(
                      child: Text(
                        topic.name,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 19,
                          height: 1.2,
                          fontWeight: FontWeight.w700,
                          color: ink,
                        ),
                      ),
                    ),
                    if (topic.credit.isNotEmpty)
                      Text(
                        topic.credit,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 12,
                          color: ink.withValues(alpha: 0.82),
                        ),
                      ),
                    const SizedBox(height: 2),
                    Text(
                      topic.verb,
                      style: TextStyle(
                        fontSize: 10.5,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.8,
                        color: ink.withValues(alpha: 0.75),
                      ),
                    ),
                  ],
                ),
              ),
              Positioned(
                top: 4,
                right: 4,
                child: IconButton(
                  tooltip: 'Remove from the Cabinet',
                  onPressed: onRemove,
                  icon: Icon(Icons.close_rounded, size: 17, color: ink.withValues(alpha: 0.7)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showDetail(BuildContext context, Topic topic) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(topic.name),
        content: SizedBox(
          width: 460,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (topic.credit.isNotEmpty)
                  Text(
                    topic.credit,
                    style: TextStyle(
                      fontSize: 12.5,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                if (topic.teaser.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(topic.teaser),
                ],
                if (topic.instruction.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(
                    topic.instruction,
                    style: const TextStyle(fontSize: 12.5, height: 1.5),
                  ),
                ],
                if (topic.target.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(
                    '${topic.verb} · ${topic.target}',
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                  ),
                ],
                const SizedBox(height: 14),
                Text(
                  'Kept on ${_date(savedAt)}',
                  style: TextStyle(
                    fontSize: 11,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }
}

class _EmptyCabinet extends StatelessWidget {
  const _EmptyCabinet({required this.hasCatalog});

  final bool hasCatalog;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('The Cabinet is empty', style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 6),
          Text(
            hasCatalog
                ? 'Spin a card and press Keep it — what you keep stays here, in '
                    'this device\'s own folder, after the window closes.'
                : 'Once the catalog is bundled, spin a card and press Keep it.',
            style: TextStyle(
              fontSize: 13,
              height: 1.5,
              color: scheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
