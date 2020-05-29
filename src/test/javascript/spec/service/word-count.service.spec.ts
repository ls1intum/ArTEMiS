import { WordCountService } from 'app/exercises/text/participate/word-count.service';

describe('WordCountService', () => {
    let service: WordCountService;

    const TEXT_WITH_63_WORDS =
        'Deutsches Ipsum Dolor quo Grimms Märchen posidonium Kaftfahrzeug-Haftpflichtversicherung adhuc Schnaps sadipscing Krankenschwester at, ' +
        'Sprechen Sie deutsch mei Handschuh gloriatur. Freude schöner Götterfunken inermis Apfelstrudel accommodare Berlin Id Angela Merkel assum Fernweh te ' +
        'Fernweh erroribus bitte Nec Bildung amet Guten Tag iriure, Joachim Löw gloriatur Aperol Spritz ut. Brezel virtute Goethe per Schweinsteiger ' +
        'At 99 Luftballons sßaevola Weltanschauung An Hochzeit malorum Prost ius';

    const EMPTY_TEXT = '';

    beforeEach(() => {
        service = new WordCountService();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('German text should contain 63 words', () => {
        expect(service.countWords(TEXT_WITH_63_WORDS)).toBe(63);
    });

    it('Empty Text should contain 0 words', () => {
        expect(service.countWords(EMPTY_TEXT)).toBe(0);
    });

    it('Null should contain 0 words', () => {
        expect(service.countWords(null)).toBe(0);
    });

    it('Undefined should contain 0 words', () => {
        expect(service.countWords(undefined)).toBe(0);
    });
});
