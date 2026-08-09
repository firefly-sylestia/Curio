// Checks candidate artist names against used set (main file + existing batches).
const fs = require('fs');
const norm = n => String(n || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
const used = new Set();
const main = JSON.parse(fs.readFileSync('app/src/main/assets/topics/artists.json', 'utf8'));
for (const t of main) used.add(norm(t.name));
for (const f of fs.readdirSync('scripts/batches').filter(x => x.startsWith('artists_') && x.endsWith('.json'))) {
  for (const t of JSON.parse(fs.readFileSync('scripts/batches/' + f, 'utf8'))) used.add(norm(t.name));
}
const candidates = [
  'Taj Mahal', 'Jimmie Rodgers', 'Hank Williams Sr.', 'The Carter Family', 'Doc Watson',
  'Earl Scruggs', 'Bill Monroe', 'Ralph Stanley', 'Dolly Parton', 'Merle Haggard',
  'Johnny Cash', 'Patsy Cline', 'Loretta Lynn', 'George Jones', 'Waylon Jennings',
  'Willie Nelson', 'Kris Kristofferson', 'Tammy Wynette', 'Hank Williams Jr.', 'Garth Brooks',
  'Shania Twain', 'Alan Jackson', 'George Strait', 'Reba McEntire', 'Dixie Chicks',
  'Emmylou Harris', 'Gillian Welch', 'Chris Stapleton', 'Kacey Musgraves', 'Sturgill Simpson',
  'Colter Wall', 'Charley Patton', 'Robert Johnson', 'Son House', 'Mississippi John Hurt',
  'Lead Belly', 'Muddy Waters', 'Howlin Wolf', 'John Lee Hooker', 'Elmore James',
  'Albert King', 'Freddie King', 'Buddy Guy', 'Stevie Ray Vaughan', 'B.B. King',
  'Etta James', 'Koko Taylor', 'Nina Simone', 'Mahalia Jackson', 'Sam Cooke',
  'Otis Redding', 'Wilson Pickett', 'James Brown', 'Sly and the Family Stone', 'George Clinton',
  'Parliament-Funkadelic', 'Bootsy Collins', 'Chaka Khan', 'Rick James', 'Teena Marie',
  'Miles Davis', 'John Coltrane', 'Thelonious Monk', 'Charlie Parker', 'Dizzy Gillespie',
  'Duke Ellington', 'Count Basie', 'Benny Goodman', 'Glenn Miller', 'Louis Armstrong',
  'Ella Fitzgerald', 'Billie Holiday', 'Sarah Vaughan', 'Dinah Washington', 'Nancy Wilson',
  'Herbie Hancock', 'Wayne Shorter', 'Weather Report', 'Return to Forever', 'Chick Corea',
  'John McLaughlin', 'Mahavishnu Orchestra', 'Al Di Meola', 'Pat Metheny', 'Bill Frisell',
  'John Zorn', 'Ornette Coleman', 'Cecil Taylor', 'Sun Ra', 'Alice Coltrane',
  'Pharoah Sanders', 'Archie Shepp', 'Eric Dolphy', 'Rahsaan Roland Kirk', 'Art Blakey'
];
for (const p of candidates) {
  const n = norm(p);
  console.log((used.has(n) ? 'USED  ' : 'FREE  ') + p);
  if (!used.has(n)) used.add(n);
}
