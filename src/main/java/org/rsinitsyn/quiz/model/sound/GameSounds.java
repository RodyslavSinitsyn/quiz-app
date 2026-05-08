package org.rsinitsyn.quiz.model.sound;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.rsinitsyn.quiz.component.custom.Emoji.HEART;
import static org.rsinitsyn.quiz.component.custom.Emoji.LAUGH;
import static org.rsinitsyn.quiz.model.sound.SoundCategory.FUNNY;

public final class GameSounds {

    public static final List<GameSound> ALL = allSounds();
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
    private static final Iterator<GameSound> CYCLE = Iterables.cycle(ALL).iterator();

    static {
        final var loaded = load();
        final var defined = allSounds();
        if (loaded.size() != defined.size()) {
            throw new IllegalStateException("Sounds are not defined");
        }
    }

    private static List<GameSound> load() {
        try (final var stream = Files.list(Path.of("src/main/resources/audio/static/sounds"))) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> new GameSound(
                            path.getFileName().toString(),
                            LAUGH,
                            FUNNY
                    ))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load sounds", e);
        }
    }

    private static List<GameSound> allSounds() {
        return Arrays.stream(SoundsHolder.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType() == GameSound.class)
                .map(field -> {
                    try {
                        return (GameSound) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    public static GameSound next() {
        return CYCLE.next();
    }

    public static GameSound random() {
        return ALL.get(RANDOM.nextInt(ALL.size()));
    }

    public static List<GameSound> random(final int count) {
        if (count <= 0) {
            return List.of();
        }
        if (count >= ALL.size()) {
            return ALL;
        }
        final var shuffled = new ArrayList<>(ALL);
        Collections.shuffle(shuffled, RANDOM);
        return shuffled.subList(0, count);
    }

    static class SoundsHolder {
        final static GameSound zelyaRozbinik = new GameSound("zelya_rozbinik.mp3", HEART, FUNNY);
        final static GameSound baza415 = new GameSound("415_baza.mp3", HEART, FUNNY);
        final static GameSound dedPoshutil = new GameSound("ded_poshutil.mp3", HEART, FUNNY);
        final static GameSound naChile = new GameSound("na_chile.mp3", HEART, FUNNY);
        final static GameSound bobrKurwa = new GameSound("bobr_kurwa.mp3", HEART, FUNNY);
        final static GameSound nichegoStrashnogoKay = new GameSound("nichego_strashnogo_kay.mp3", HEART, FUNNY);
        final static GameSound dobryak = new GameSound("dobryak.mp3", HEART, FUNNY);
        final static GameSound chinazes = new GameSound("chinazes.mp3", HEART, FUNNY);
        final static GameSound ymneeChemKomp = new GameSound("ymnee_chem_komp.mp3", HEART, FUNNY);
        final static GameSound volkDiplom = new GameSound("volk_diplom.mp3", HEART, FUNNY);
        final static GameSound kitMamuMav = new GameSound("kit_mamu_mav.mp3", HEART, FUNNY);
        final static GameSound okLetsgo = new GameSound("ok_letsgo.mp3", HEART, FUNNY);
        final static GameSound tehnikPodskaz = new GameSound("tehnik_podskaz.mp3", HEART, FUNNY);
        final static GameSound podnyatsa = new GameSound("podnyatsa.mp3", HEART, FUNNY);
        final static GameSound robotPizza = new GameSound("robot_pizza.mp3", HEART, FUNNY);
        final static GameSound yanikForget = new GameSound("yanik_forget.mp3", HEART, FUNNY);
        final static GameSound klichkoDen = new GameSound("klichko_den.mp3", HEART, FUNNY);
        final static GameSound neFartanulo = new GameSound("ne_fartanulo.mp3", HEART, FUNNY);
        final static GameSound minus3 = new GameSound("minus_3.mp3", HEART, FUNNY);
        final static GameSound mb16Pamati = new GameSound("16_mb_pamati.mp3", HEART, FUNNY);
        final static GameSound livsiPlan = new GameSound("livsi_plan.mp3", HEART, FUNNY);
        final static GameSound neZval = new GameSound("ne_zval.mp3", HEART, FUNNY);
        final static GameSound kernesNaNol = new GameSound("kernes_na_nol.mp3", HEART, FUNNY);
        final static GameSound tehnikDvijenie = new GameSound("tehnik_dvijenie.mp3", HEART, FUNNY);
        final static GameSound dobkin = new GameSound("dobkin.mp3", HEART, FUNNY);
        final static GameSound kalmarUjeIgral = new GameSound("kalmar_uje_igral.mp3", HEART, FUNNY);
        final static GameSound lyaskoKomynaky = new GameSound("lyasko_komynaky.mp3", HEART, FUNNY);
        final static GameSound pidpis = new GameSound("pidpis.mp3", HEART, FUNNY);
        final static GameSound polskaKorova = new GameSound("polska_korova.mp3", HEART, FUNNY);
        final static GameSound gorinBoje = new GameSound("gorin_boje.mp3", HEART, FUNNY);
        final static GameSound chipiChipi = new GameSound("chipi_chipi.mp3", HEART, FUNNY);
        final static GameSound yanikStandup = new GameSound("yanik_standup.mp3", HEART, FUNNY);
        final static GameSound dich = new GameSound("dich.mp3", HEART, FUNNY);
        final static GameSound weChtoTypie = new GameSound("we_chto_typie.mp3", HEART, FUNNY);
        final static GameSound penguinsMashem = new GameSound("penguins_mashem.mp3", HEART, FUNNY);
        final static GameSound gorinKamish = new GameSound("gorin_kamish.mp3", HEART, FUNNY);
        final static GameSound polnomochia = new GameSound("polnomochia.mp3", HEART, FUNNY);
        final static GameSound sueta = new GameSound("sueta.mp3", HEART, FUNNY);
        final static GameSound obosralsa = new GameSound("obosralsa.mp3", HEART, FUNNY);
        final static GameSound tehnikPeretupal = new GameSound("tehnik_peretupal.mp3", HEART, FUNNY);
        final static GameSound minut510 = new GameSound("minut_5_10.mp3", HEART, FUNNY);
        final static GameSound yulaPropalo = new GameSound("yula_propalo.mp3", HEART, FUNNY);
        final static GameSound lovkoPridumal = new GameSound("lovko_pridumal.mp3", HEART, FUNNY);
        final static GameSound povezloPovezlo = new GameSound("povezlo_povezlo.mp3", HEART, FUNNY);
        final static GameSound zelenskyHtoya = new GameSound("zelensky_htoya.mp3", HEART, FUNNY);
        final static GameSound ogBebra = new GameSound("og_bebra.mp3", HEART, FUNNY);
        final static GameSound yaz = new GameSound("yaz.mp3", HEART, FUNNY);
    }
}
