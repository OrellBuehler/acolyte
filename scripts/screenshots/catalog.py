"""Invented titles for the store screenshots. Nothing here exists: names, plots,
people and artwork are made up (see art/NOTICE.md)."""

PALETTES = {
    "dusk": ("#1b1f4b", "#e0654f", "#ffd9a0"),
    "teal": ("#06323a", "#2c9c8f", "#c9f2e4"),
    "plum": ("#2a0f3a", "#a63c7a", "#ffc8e3"),
    "ember": ("#2b0d0d", "#e8742c", "#ffe2b0"),
    "ocean": ("#0a1d3d", "#2f6fcb", "#bfe0ff"),
    "moss": ("#0f2a1c", "#6fa84a", "#e6f5c0"),
    "sand": ("#3a2a14", "#d9a441", "#fff0c4"),
    "slate": ("#161a26", "#5a6b8c", "#dfe6f5"),
    "rose": ("#3a0f1d", "#d2557a", "#ffd6df"),
    "ice": ("#0d2433", "#7fc4de", "#f0fbff"),
}

TITLES = [
    # slug, kind, library id, tmdb, name, year, palette, motif, genres (en, de), runtime / episode runtime, score, rating
    dict(slug="harbor-lights", kind="movie", id=1, tmdb="910001", name="Harbor Lights", year=2024, palette="dusk", motif="sun",
         genres=("Drama", "Drama"), runtime=118, score=7.8, rating="PG-13",
         overview=("A night-shift ferry pilot and a visiting lighthouse engineer have one winter to decide whether the old harbor "
                   "is worth saving."),
         overview_de=("Eine Nachtfähre-Kapitänin und ein Leuchtturm-Ingenieur haben einen Winter Zeit, um zu entscheiden, "
                      "ob der alte Hafen zu retten ist.")),
    dict(slug="the-last-lantern", kind="movie", id=2, tmdb="910002", name="The Last Lantern", year=2023, palette="plum", motif="lantern",
         genres=("Fantasy", "Fantasy"), runtime=126, score=7.4, rating="PG",
         overview="When the final lantern in the valley flickers out, a young cartographer follows the dark to its source.",
         overview_de=("Als die letzte Laterne im Tal erlischt, folgt eine junge Kartografin der Dunkelheit bis zu ihrem "
                      "Ursprung.")),
    dict(slug="paper-moons", kind="movie", id=3, tmdb="910003", name="Paper Moons", year=2025, palette="rose", motif="moon",
         genres=("Animation", "Animation"), runtime=94, score=8.1, rating="G",
         overview="Two siblings fold a new moon out of paper every night, until the real one starts to copy them.",
         overview_de=("Zwei Geschwister falten jede Nacht einen neuen Mond aus Papier, bis der echte anfängt, sie "
                      "nachzuahmen.")),
    dict(slug="orbit-nine", kind="movie", id=4, tmdb="910004", name="Orbit Nine", year=2025, palette="ocean", motif="rings",
         genres=("Science Fiction", "Science-Fiction"), runtime=132, score=7.6, rating="PG-13",
         overview="A maintenance crew on the ninth ring of a failing station has six hours to reroute the sun.",
         overview_de=("Eine Wartungscrew auf dem neunten Ring einer verfallenden Station hat sechs Stunden, um die Sonne "
                      "umzuleiten.")),
    dict(slug="velvet-static", kind="movie", id=5, tmdb="910005", name="Velvet Static", year=2022, palette="slate", motif="city",
         genres=("Thriller", "Thriller"), runtime=109, score=7.0, rating="R",
         overview="A radio host hears her own voice on a frequency that does not exist, and it knows tomorrow's headlines.",
         overview_de=("Eine Radiomoderatorin hört ihre eigene Stimme auf einer Frequenz, die es nicht gibt, und die kennt "
                      "die Schlagzeilen von morgen.")),
    dict(slug="salt-and-ember", kind="movie", id=6, tmdb="910006", name="Salt & Ember", year=2024, palette="ember", motif="waves",
         genres=("Adventure", "Abenteuer"), runtime=121, score=7.3, rating="PG-13",
         overview="A salvage crew races a rival ship across a frozen sea to reach a wreck that holds a very warm secret.",
         overview_de=("Eine Bergungscrew liefert sich ein Rennen über ein gefrorenes Meer, um ein Wrack mit einem sehr "
                      "warmen Geheimnis zu erreichen.")),
    dict(slug="midnight-orchard", kind="movie", id=7, tmdb="910007", name="Midnight Orchard", year=2021, palette="moss", motif="trees",
         genres=("Mystery", "Mystery"), runtime=104, score=6.9, rating="PG-13",
         overview="Every apple in the orchard ripens at midnight, and every year one villager disappears with the harvest.",
         overview_de=("Jeder Apfel im Obstgarten reift um Mitternacht, und jedes Jahr verschwindet ein Dorfbewohner mit "
                      "der Ernte.")),
    dict(slug="copper-canyon", kind="movie", id=8, tmdb="910008", name="Copper Canyon", year=2023, palette="sand", motif="mesa",
         genres=("Western", "Western"), runtime=115, score=7.2, rating="PG-13",
         overview="A retired surveyor is hired to map a canyon that keeps changing shape after dark.",
         overview_de=("Ein pensionierter Landvermesser soll einen Canyon kartieren, der nach Einbruch der Dunkelheit "
                      "ständig seine Form ändert.")),
    dict(slug="glasshouse", kind="show", id=11, tmdb="920001", name="Glasshouse", year=2023, palette="teal", motif="panes",
         genres=("Drama", "Drama"), runtime=48, score=8.3, rating="TV-14", seasons=3, episodes=(10, 10, 8),
         overview="Four tenants of a converted greenhouse discover that the building remembers everything said inside it.",
         overview_de=("Vier Mieter eines umgebauten Gewächshauses entdecken, dass das Gebäude sich an alles erinnert, was "
                      "darin gesagt wird.")),
    dict(slug="night-shift-diaries", kind="show", id=12, tmdb="920002", name="Night Shift Diaries", year=2024, palette="plum",
         motif="stars", genres=("Comedy", "Komödie"), runtime=28, score=7.9, rating="TV-14", seasons=2, episodes=(8, 8),
         overview="The overnight staff of a very small hospital deal with the very big problems of very small hours.",
         overview_de=("Das Nachtpersonal eines sehr kleinen Spitals kämpft mit den sehr grossen Problemen der sehr kleinen "
                      "Stunden.")),
    dict(slug="tidewater", kind="show", id=13, tmdb="920003", name="Tidewater", year=2022, palette="ocean", motif="waves",
         genres=("Crime", "Krimi"), runtime=52, score=8.0, rating="TV-MA", seasons=4, episodes=(8, 8, 10, 6),
         overview="A coastal detective works cold cases that only the tide brings back to the surface.",
         overview_de="Eine Küstendetektivin löst alte Fälle, die nur die Flut wieder an die Oberfläche bringt."),
    dict(slug="the-cartographers", kind="show", id=14, tmdb="920004", name="The Cartographers", year=2025, palette="sand",
         motif="mesa", genres=("Adventure", "Abenteuer"), runtime=44, score=7.7, rating="TV-PG", seasons=1, episodes=(10,),
         overview="A guild of mapmakers sets out to chart a continent that was never supposed to exist.",
         overview_de=("Eine Gilde von Kartenzeichnern macht sich auf, einen Kontinent zu vermessen, den es eigentlich nie "
                      "geben sollte.")),
    dict(slug="northern-lines", kind="show", id=15, tmdb="920005", name="Northern Lines", year=2021, palette="ice", motif="city",
         genres=("Drama", "Drama"), runtime=46, score=7.5, rating="TV-14", seasons=2, episodes=(8, 8),
         overview="Commuters on a single overnight train line slowly become the family none of them had.",
         overview_de=("Pendler einer einzigen Nachtzuglinie werden langsam zu der Familie, die keiner von ihnen hatte.")),
    dict(slug="quiet-machines", kind="movie", id=0, tmdb="910021", name="Quiet Machines", year=2026, palette="slate", motif="rings",
         genres=("Science Fiction", "Science-Fiction"), runtime=112, score=7.1, rating="PG-13",
         overview="In a city where machines have learned to be polite, one of them finally refuses.",
         overview_de="In einer Stadt, in der Maschinen höflich geworden sind, weigert sich endlich eine.",
         upcoming=True),
    dict(slug="ember-tide", kind="movie", id=0, tmdb="910022", name="Ember Tide", year=2025, palette="ember", motif="sun",
         genres=("Adventure", "Abenteuer"), runtime=117, score=7.0, rating="PG-13",
         overview="Every evening the sea turns the color of fire, and every evening someone swims out to meet it.",
         overview_de=("Jeden Abend färbt sich das Meer feuerrot, und jeden Abend schwimmt jemand hinaus, um es zu "
                      "treffen.")),
    dict(slug="lowlight", kind="show", id=0, tmdb="920021", name="Lowlight", year=2025, palette="plum", motif="city",
         genres=("Thriller", "Thriller"), runtime=45, score=7.8, rating="TV-MA", seasons=1, episodes=(8,),
         overview="A night-market photographer realizes her pictures show people a few seconds before they vanish.",
         overview_de=("Eine Nachtmarkt-Fotografin merkt, dass ihre Bilder Menschen zeigen, wenige Sekunden bevor sie "
                      "verschwinden.")),
    dict(slug="starling-court", kind="show", id=0, tmdb="920022", name="Starling Court", year=2024, palette="rose", motif="stars",
         genres=("Drama", "Drama"), runtime=50, score=8.2, rating="TV-14", seasons=2, episodes=(8, 8),
         overview="Three generations of a family law firm argue, scheme and occasionally win.",
         overview_de=("Drei Generationen einer Familienkanzlei streiten, intrigieren und gewinnen gelegentlich.")),
    dict(slug="the-understudy", kind="movie", id=0, tmdb="910023", name="The Understudy", year=2025, palette="dusk", motif="lantern",
         genres=("Comedy", "Komödie"), runtime=101, score=7.5, rating="PG-13",
         overview="A theater understudy gets one night on stage, and the whole company conspires to make it unforgettable.",
         overview_de=("Eine Theater-Zweitbesetzung bekommt einen Abend auf der Bühne, und das ganze Ensemble sorgt dafür, "
                      "dass er unvergesslich wird.")),
    dict(slug="fjord-and-fire", kind="movie", id=0, tmdb="910024", name="Fjord & Fire", year=2024, palette="teal", motif="mesa",
         genres=("Adventure", "Abenteuer"), runtime=123, score=7.3, rating="PG-13",
         overview="A ferry captain and a volcanologist share one very small boat and one very large problem.",
         overview_de=("Eine Fährkapitänin und ein Vulkanologe teilen sich ein sehr kleines Boot und ein sehr grosses "
                      "Problem.")),
    dict(slug="moth-and-lamp", kind="movie", id=0, tmdb="910025", name="Moth & Lamp", year=2026, palette="moss", motif="moon",
         genres=("Animation", "Animation"), runtime=88, score=7.9, rating="G",
         overview="A moth who is afraid of light moves into a lighthouse.",
         overview_de="Eine Motte, die sich vor Licht fürchtet, zieht in einen Leuchtturm.",
         upcoming=True),
]

PEOPLE = [
    "Mara Venn", "Teodor Hale", "Iris Calloway", "Joaquin Reyes", "Nell Okafor", "Bram Lindqvist", "Sana Whitlock", "Oren Castellan",
]

BY_SLUG = {t["slug"]: t for t in TITLES}
BY_TMDB = {t["tmdb"]: t for t in TITLES}
BY_ID = {t["id"]: t for t in TITLES if t["id"]}
