import 'package:flutter/material.dart';

import '../data/journal.dart';
import '../data/lanes.dart';
import '../data/topic.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_widgets.dart';
import 'journal_editor.dart';

/// The Cabinet: what you kept, and what you wrote.
///
/// Two tabs, because the phone puts both in the same room: **Discoveries** (the
/// kept cards) and **Journals** (the pages). An open page takes the whole room —
/// a writing surface behind a tab bar is a writing surface with a strip of
/// furniture over it.
class CabinetScreen extends StatefulWidget {
  const CabinetScreen({super.key, required this.model});

  final AppModel model;

  @override
  State<CabinetScreen> createState() => _CabinetScreenState();
}

class _CabinetScreenState extends State<CabinetScreen> {
  int _tab = 0;

  /// The Cabinet's own search and lane filter. The phone's Cabinet has both
  /// (search over the kept rows, chips by category) and a collection you cannot
  /// search is a pile — a kept card you have to scroll for is a kept card you
  /// will not find again.
  final TextEditingController _search = TextEditingController();
  String? _laneId;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final model = widget.model;
    final open = model.openJournal;
    if (open != null) {
      return JournalEditor(model: model, entry: open);
    }
    final scheme = Theme.of(context).colorScheme;
    return PageBody(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: 'Cabinet',
            subtitle: _tab == 0
                ? (model.saved.isEmpty
                    ? 'Nothing kept yet — a card you spin can be kept here.'
                    : '${model.saved.length} ${model.saved.length == 1 ? 'discovery' : 'discoveries'} kept')
                : (model.journals.isEmpty
                    ? 'No pages yet. A journal is yours alone, kept on this device.'
                    : '${model.journals.length} ${model.journals.length == 1 ? 'page' : 'pages'} · ${model.journalWords} words'),
            trailing: _tab == 0
                ? (model.saved.isEmpty
                    ? null
                    : TextButton.icon(
                        onPressed: () => _confirmClear(context),
                        icon: const Icon(Icons.delete_sweep_outlined, size: 18),
                        label: const Text('Clear'),
                      ))
                : FilledButton.icon(
                    onPressed: model.createJournal,
                    icon: const Icon(Icons.add_rounded, size: 18),
                    label: const Text('New page'),
                  ),
          ),
          const SizedBox(height: 14),
          SegmentedButton<int>(
            segments: const [
              ButtonSegment(value: 0, label: Text('Discoveries')),
              ButtonSegment(value: 1, label: Text('Journals')),
            ],
            selected: <int>{_tab},
            onSelectionChanged: (selection) => setState(() => _tab = selection.first),
          ),
          const SizedBox(height: CurioSpace.gap + 4),
          if (_tab == 0) _discoveries(context) else _journals(context),
          if (_tab == 0 && model.saved.isNotEmpty) ...[
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

  Widget _discoveries(BuildContext context) {
    final model = widget.model;
    if (model.saved.isEmpty) return _EmptyCabinet(hasCatalog: !model.catalogEmpty);
    final scheme = Theme.of(context).colorScheme;
    final query = _search.text.trim().toLowerCase();
    final rows = model.saved.where((entry) {
      if (_laneId != null && _laneOf(entry.topic) != _laneId) return false;
      if (query.isEmpty) return true;
      final topic = entry.topic;
      return topic.name.toLowerCase().contains(query) ||
          topic.credit.toLowerCase().contains(query) ||
          topic.teaser.toLowerCase().contains(query) ||
          topic.tags.any((tag) => tag.toLowerCase().contains(query));
    }).toList(growable: false);
    final laneIds = <String>{for (final entry in model.saved) _laneOf(entry.topic)};

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: _search,
          onChanged: (_) => setState(() {}),
          style: const TextStyle(fontSize: 13),
          decoration: InputDecoration(
            isDense: true,
            hintText: 'Search what you kept',
            prefixIcon: const Icon(Icons.search_rounded, size: 17),
            prefixIconConstraints: const BoxConstraints(minWidth: 38, minHeight: 38),
            suffixIcon: _search.text.isEmpty
                ? null
                : IconButton(
                    tooltip: 'Clear the search',
                    onPressed: () => setState(() => _search.clear()),
                    icon: const Icon(Icons.close_rounded, size: 15),
                  ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(999),
              borderSide: BorderSide(color: scheme.outlineVariant),
            ),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            LaneChip(
              label: 'All',
              accent: 0xFFB08A86,
              selected: _laneId == null,
              onTap: () => setState(() => _laneId = null),
            ),
            for (final id in laneIds)
              LaneChip(
                label: laneById(id).label,
                accent: laneById(id).accent,
                selected: _laneId == id,
                onTap: () => setState(() => _laneId = _laneId == id ? null : id),
              ),
          ],
        ),
        const SizedBox(height: CurioSpace.gap + 4),
        if (rows.isEmpty)
          Text(
            query.isEmpty
                ? 'Nothing kept in that lane yet.'
                : 'Nothing matches “${_search.text.trim()}”.',
            style: TextStyle(fontSize: 13, color: scheme.onSurfaceVariant),
          )
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
            itemCount: rows.length,
            itemBuilder: (context, index) {
              final entry = rows[index];
              return _SavedCard(
                topic: entry.topic,
                savedAt: entry.savedAt,
                onRemove: () => model.remove(entry.topic.id),
              );
            },
          ),
      ],
    );
  }

  /// A kept topic's lane — the same mapping the saved card itself draws with.
  static String _laneOf(Topic topic) =>
      laneById(topic.categoryId.toLowerCase().replaceAll('_', '-')).id;

  Widget _journals(BuildContext context) {
    final model = widget.model;
    if (model.journals.isEmpty) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(28),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Your pages', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 6),
            Text(
              'A page is written for yourself: headings, quotes, checklists, a '
              'mood and tags. It stays on this device, in the same folder as the '
              'Cabinet.',
              style: TextStyle(
                fontSize: 13,
                height: 1.5,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: model.createJournal,
              icon: const Icon(Icons.add_rounded, size: 18),
              label: const Text('Write the first page'),
            ),
          ],
        ),
      );
    }
    return Column(
      children: [
        for (final entry in model.journals)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: _JournalRow(
              entry: entry,
              onOpen: () => model.openJournalById(entry.id),
              onDelete: () => _confirmDeleteJournal(context, entry),
            ),
          ),
      ],
    );
  }

  Future<void> _confirmDeleteJournal(BuildContext context, JournalEntry entry) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Delete “${entry.displayTitle}”?'),
        content: const Text('The page is removed from this device. This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Keep it'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed ?? false) await widget.model.deleteJournal(entry.id);
  }

  Future<void> _confirmClear(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear the Cabinet?'),
        content: const Text('Every kept discovery is removed. This cannot be undone.'),
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
    if (confirmed ?? false) await widget.model.clearCabinet();
  }
}

/// One page in the Journals list: its title, when it was last written, how long
/// it is, and the mood it was left in.
class _JournalRow extends StatelessWidget {
  const _JournalRow({
    required this.entry,
    required this.onOpen,
    required this.onDelete,
  });

  final JournalEntry entry;
  final VoidCallback onOpen;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onOpen,
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
        child: Container(
          padding: const EdgeInsets.fromLTRB(20, 16, 10, 16),
          decoration: BoxDecoration(
            color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
            borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
          ),
          child: Row(
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    JournalEntry.moodGlyphs[entry.mood.clamp(0, 4).toInt()],
                    style: TextStyle(
                      fontSize: 20,
                      color: entry.mood == 0 ? scheme.onSurfaceVariant : scheme.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      entry.displayTitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 15.5, fontWeight: FontWeight.w700),
                    ),
                    if (entry.preview.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        entry.preview,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 12.5,
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Text(
                '${entry.words} ${entry.words == 1 ? 'word' : 'words'} · ${_when(entry.updatedAt)}',
                style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
              ),
              IconButton(
                tooltip: 'Delete this page',
                onPressed: onDelete,
                icon: const Icon(Icons.delete_outline_rounded, size: 17),
              ),
            ],
          ),
        ),
      ),
    );
  }

  static String _when(DateTime value) {
    final local = value.toLocal();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final day = DateTime(local.year, local.month, local.day);
    final difference = today.difference(day).inDays;
    if (difference == 0) return 'today';
    if (difference == 1) return 'yesterday';
    if (difference < 7) return '$difference days ago';
    const months = <String>[
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
    ];
    return '${local.day} ${months[local.month - 1]}';
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
