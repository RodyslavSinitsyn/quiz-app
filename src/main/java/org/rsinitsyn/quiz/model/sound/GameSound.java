package org.rsinitsyn.quiz.model.sound;

import org.rsinitsyn.quiz.component.custom.Emoji;

public record GameSound(String path, Emoji emoji, SoundCategory category) {

    public String fullPath() {
        return "sounds/%s".formatted(path);
    }

    /**
     *zelya_rozbinik.mp3
     * 415_baza.mp3
     * ded_poshutil.mp3
     * na_chile.mp3
     * bobr_kurwa.mp3
     * nichego_strashnogo_kay.mp3
     * dobryak.mp3
     * chinazes.mp3
     * ymnee_chem_komp.mp3
     * volk_diplom.mp3
     * kit_mamu_mav.mp3
     * ok_letsgo.mp3
     * tehnik_podskaz.mp3
     * podnyatsa.mp3
     * robot_pizza.mp3
     * yanik_forget.mp3
     * klichko_den.mp3
     * ne_fartanulo.mp3
     * minus_3.mp3
     * 16_mb_pamati.mp3
     * livsi_plan.mp3
     * ne_zval.mp3
     * kernes_na_nol.mp3
     * tehnik_dvijenie.mp3
     * dobkin.mp3
     * kalmar_uje_igral.mp3
     * lyasko_komynaky.mp3
     * pidpis.mp3
     * polska_korova.mp3
     * gorin_boje.mp3
     * chipi_chipi.mp3
     * yanik_standup.mp3
     * dich.mp3
     * we_chto_typie.mp3
     * penguins_mashem.mp3
     * gorin_kamish.mp3
     * polnomochia.mp3
     * sueta.mp3
     * obosralsa.mp3
     * tehnik_peretupal.mp3
     * minut_5_10.mp3
     * yula_propalo.mp3
     * lovko_pridumal.mp3
     * povezlo_povezlo.mp3
     * zelensky_htoya.mp3
     * og_bebra.mp3
     * yaz.mp3
     */
}
