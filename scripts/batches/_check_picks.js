// Checks candidate artwork names against used set (main file + existing batches).
const fs = require('fs');
const norm = n => String(n || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
const used = new Set();
const main = JSON.parse(fs.readFileSync('app/src/main/assets/topics/artworks.json', 'utf8'));
for (const t of main) used.add(norm(t.name));
for (const f of fs.readdirSync('scripts/batches').filter(x => x.startsWith('artworks_') && x.endsWith('.json'))) {
  for (const t of JSON.parse(fs.readFileSync('scripts/batches/' + f, 'utf8'))) used.add(norm(t.name));
}
const candidates = [
  'The Hay Wain (1821)', 'The Gross Clinic (1875)', 'Portrait of Madame X (1884)',
  'The Card Players (1893)', 'Olympia (1863)', 'The Burial at Ornans (1850)',
  'The Avenue at Middelharnis (1689)', 'Las Meninas (1656)', 'The Rokeby Venus (1647)',
  'The Toilet of Venus (1651)', 'Judith Beheading Holofernes (1599)', 'The Calling of Saint Matthew (1600)',
  'Supper at Emmaus (1601)', 'The Beheading of Saint John the Baptist (1608)', 'David with the Head of Goliath (1610)',
  'The Entombment of Christ (1603)', 'Bacchus (1596)', 'The Lute Player (1596)', 'Medusa (1597)',
  'The Fortune Teller (1594)', 'Boy Bitten by a Lizard (1594)', 'The Musicians (1595)',
  'The Crowning with Thorns (1602)', 'The Taking of Christ (1602)', 'The Incredulity of Saint Thomas (1602)',
  'The Seven Works of Mercy (1607)', 'The Flagellation (1607)', 'Salome with the Head of John the Baptist (1609)',
  'Narcissus (1599)', 'The Tooth Puller (1608)', 'Saint Jerome Writing (1606)', 'The Sacrifice of Isaac (1603)',
  'The Denial of Saint Peter (1610)', 'The Conversion of Saint Paul (1601)', 'The Crucifixion of Saint Peter (1601)',
  'The Deposition (1604)', 'Amor Victorious (1602)', 'Boy with a Basket of Fruit (1593)',
  'Young Sick Bacchus (1593)', 'The Cardsharps (1595)', 'Rest on the Flight into Egypt (1597)',
  'Judith and Holofernes (1612)', 'Salome Receives the Head (1607)', 'The Adoration of the Shepherds (1609)',
  'The Martyrdom of Saint Matthew (1600)', 'Saint Francis in Meditation (1606)', 'The Annunciation (1608)',
  'David and Goliath (1599)', 'The Entombment (1602)', 'The Calling of the Apostles (1608)',
  'Saint John the Baptist (1602)', 'The Musicians of the Four Seasons (1612)', 'The Burial of Saint Lucy (1608)',
  'The Resurrection of Lazarus (1609)', 'The Flagellation of Christ (1607)', 'Salome (1609)',
  'The Adoration of the Magi (1609)', 'Saint Ursula (1610)', 'The Crowning with Thorns (1607)',
  'Christ at the Column (1607)', 'The Penitent Magdalene (1597)', 'The Annunciation (1609)',
  'The Beheading of Saint John (1608)', 'The Lute Player (1600)', 'The Martyrdom of Saint Ursula (1610)',
  'The Entombment of Christ (1604)', 'The Supper at Emmaus (1606)', 'The Denial of Peter (1610)',
  'The Calling of Saint Matthew (1599)', 'The Conversion of Mary Magdalene (1598)', 'Judith (1612)',
  'The Boy with the Thorn (1500)', 'The Crucifixion (1601)', 'Saint Jerome (1607)'
];
for (const p of candidates) {
  const n = norm(p);
  console.log((used.has(n) ? 'USED  ' : 'FREE  ') + p);
  if (!used.has(n)) used.add(n);
}
