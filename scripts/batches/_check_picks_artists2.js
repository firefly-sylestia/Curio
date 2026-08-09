const fs = require('fs');
const norm = n => String(n || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
const used = new Set();
const main = JSON.parse(fs.readFileSync('app/src/main/assets/topics/artists.json', 'utf8'));
for (const t of main) used.add(norm(t.name));
for (const f of fs.readdirSync('scripts/batches').filter(x => x.startsWith('artists_') && x.endsWith('.json'))) {
  for (const t of JSON.parse(fs.readFileSync('scripts/batches/' + f, 'utf8'))) used.add(norm(t.name));
}
const candidates = [
  'Xxxtentacion', 'Lil Wayne', 'Drake', 'Kendrick Lamar', 'J. Cole', 'Travis Scott',
  'Future', 'Juice Wrld', 'Pop Smoke', 'Mac Miller', 'Chance the Rapper', 'Kid Cudi',
  'Kanye West', 'Jay-Z', 'Nas', 'The Notorious B.I.G.', 'Tupac', 'Outkast', 'Andre 3000',
  'Big Boi', 'Lauryn Hill', 'Missy Elliott', 'Nicki Minaj', 'Cardi B', 'Megan Thee Stallion',
  'Doja Cat', 'Lil Nas X', 'Jack Harlow', 'Tyler Okonma', 'Childish Gambino', 'Frank Ocean',
  'Solange', 'SZA', 'Summer Walker', 'Giveon', 'H.E.R.', 'Daniel Caesar', 'Jhené Aiko',
  'Erykah Badu', 'DAngelo', 'The Roots', 'Common', 'Mos Def', 'Talib Kweli', 'Black Star',
  'Gang Starr', 'Mobb Deep', 'Wu-Tang Clan', 'Method Man', 'Ghostface Killah', 'Raekwon',
  'Nasir Jones', 'Rakim', 'Eric B. and Rakim', 'Public Enemy', 'Run-D.M.C.', 'LL Cool J',
  'Beastie Boys', 'A Tribe Called Quest', 'De La Soul', 'Jungle Brothers', 'MF DOOM',
  'Madvillain', 'Danger Mouse', 'J Dilla', 'Madlib', 'Nujabes', 'Flying Lotus',
  'Thundercat', 'Kamasi Washington', 'Robert Glasper', 'Esperanza Spalding', 'Christian Scott',
  'Terrace Martin', 'Anderson Paak', 'Knxwledge', 'Kaytranada', 'Four Tet', 'Floating Points',
  'Jon Hopkins', 'Bonobo', 'Maribou State', 'Tycho', 'Emancipator', 'Odesza', 'Rufus Du Sol',
  'Disclosure', 'Flume', 'ODESZA', 'Porter Robinson', 'Madeon', 'Skrillex', 'Deadmau5',
  'Aphex Twin', 'Boards of Canada', 'Autechre', 'Squarepusher', 'Oneohtrix Point Never',
  'Arca', 'Sophie', 'Charli XCX', 'Grimes', 'Bjork', 'FKA Twigs', 'Bat for Lashes'
];
for (const p of candidates) {
  const n = norm(p);
  console.log((used.has(n) ? 'USED  ' : 'FREE  ') + p);
  if (!used.has(n)) used.add(n);
}
