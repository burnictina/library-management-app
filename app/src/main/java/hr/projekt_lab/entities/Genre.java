package hr.projekt_lab.entities;

public enum Genre {
    TRAGEDIJA("Tragedija"),
    EP("Ep"),
    SATIRICNI_ROMAN("Satirični roman"),
    POVIJESNI_ROMAN("Povijesni roman"),
    KLASICNI_ROMAN("Klasični roman"),
    KOMEDIJA("Komedija"),
    DRAMA("Drama"),
    LIRSKA_POEZIJA("Lirska poezija"),
    ROMAN("Roman"),
    ZNANSTVENA_FANTASTIKA("Znanstvena fantastika"),
    FANTASY("Fantasy"),
    KRIMINALISTICKI_ROMAN("Kriminalistički roman"),
    PUSTOLOVNI_ROMAN("Pustolovni roman"),
    PSIHOLOSKI_ROMAN("Psihološki roman"),
    FILOZOFSKI_ROMAN("Filozofski roman"),
    BIOGRAFIJA("Biografija"),
    AUTOBIOGRAFIJA("Autobiografija"),
    ESEJ("Esej"),
    DJECJA_KNJIZEVNOST("Dječja književnost"),
    HOROR("Horor"),
    OSTALO("Ostalo");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
