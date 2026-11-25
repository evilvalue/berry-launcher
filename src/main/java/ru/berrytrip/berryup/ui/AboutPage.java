package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Sys;
import ru.berrytrip.berryup.UpdateCheck;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Вкладка «О программе»: версия, что нового, ссылки и проверка обновлений. */
public class AboutPage extends JPanel {

    private final MainFrame host;
    private final JLabel status;

    public AboutPage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Widgets.Card card = new Widgets.Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.pad(18, 20, 18, 20);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JLabel title = Widgets.label("BerryUp " + Cfg.VERSION, Palette.semi(15f), Palette.FG);
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(3));

        JLabel sub = Widgets.label("Надстройка над Prism Launcher  ·  " + Cfg.BUILD + "  ·  " + Cfg.LOADER
                + "  ·  Minecraft " + Cfg.MC_VERSION, Palette.reg(8.6f), Palette.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(14));

        JLabel caption = Widgets.label("ЧТО НОВОГО В 3.2", Palette.semi(8f), Palette.DIM);
        caption.setAlignmentX(LEFT_ALIGNMENT);
        card.add(caption);
        card.add(Box.createVerticalStrut(6));

        String notes = "Новый дизайн окна и иконка: ягодный градиент, объёмные карточки, аккуратная навигация.\n"
                + "BerryUp по-прежнему один файл BerryUp.jar: ни установщика, ни архива, ни скриптов рядом.\n"
                + "Сборка и Prism Launcher остаются на месте: BerryUp находит инстанс, обновляет его и запускает\n"
                + "игру. Обновления BerryUp применяет сам — скачивает новый jar и подменяет файл при перезапуске.";
        JLabel notesLabel = Widgets.label("<html><div style='width:520px'>" + notes.replace("\n", "<br>") + "</div></html>",
                Palette.reg(8.8f), Palette.MUTED);
        notesLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(notesLabel);
        card.add(Box.createVerticalStrut(12));

        status = Widgets.label("Проверка обновлений — по кнопке ниже.", Palette.reg(8.4f), Palette.DIM);
        status.setAlignmentX(LEFT_ALIGNMENT);
        card.add(status);
        add(card);
        add(Box.createVerticalStrut(12));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        Widgets.BerryButton check = new Widgets.BerryButton("Проверить обновление", Widgets.BerryButton.Kind.PRIMARY);
        check.addActionListener(e -> {
            status.setText("Проверяю…");
            host.checkUpdates(true);
        });
        Widgets.BerryButton releases = new Widgets.BerryButton("Релизы", Widgets.BerryButton.Kind.SECONDARY);
        releases.addActionListener(e -> Sys.openUrl(Cfg.RELEASES));
        Widgets.BerryButton sources = new Widgets.BerryButton("Исходники", Widgets.BerryButton.Kind.GHOST);
        sources.addActionListener(e -> Sys.openUrl(Cfg.REPO));
        Widgets.BerryButton site = new Widgets.BerryButton("Сайт сборки", Widgets.BerryButton.Kind.SECONDARY);
        site.addActionListener(e -> Sys.openUrl(Cfg.SITE));
        Widgets.BerryButton telegram = new Widgets.BerryButton("Telegram", Widgets.BerryButton.Kind.GHOST);
        telegram.addActionListener(e -> Sys.openUrl(Cfg.TELEGRAM));
        Widgets.BerryButton config = new Widgets.BerryButton("Папка настроек", Widgets.BerryButton.Kind.GHOST);
        config.addActionListener(e -> Sys.openFolder(Cfg.configDir()));

        buttons.add(check);
        buttons.add(releases);
        buttons.add(sources);
        buttons.add(site);
        buttons.add(telegram);
        buttons.add(config);
        add(buttons);
        add(Box.createVerticalStrut(10));

        JLabel note = Widgets.label("BerryUp не заменяет Prism Launcher: без него игра не запустится.",
                Palette.reg(8.4f), Palette.DIM);
        note.setAlignmentX(LEFT_ALIGNMENT);
        add(note);

        JPanel prismLinks = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        prismLinks.setOpaque(false);
        prismLinks.setAlignmentX(LEFT_ALIGNMENT);
        prismLinks.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        Widgets.BerryButton prism = new Widgets.BerryButton("Скачать Prism Launcher", Widgets.BerryButton.Kind.SECONDARY);
        prism.addActionListener(e -> Sys.openUrl(Cfg.PRISM_URL));
        Widgets.BerryButton pine = new Widgets.BerryButton("Скачать PineconeMC", Widgets.BerryButton.Kind.GHOST);
        pine.addActionListener(e -> Sys.openUrl(Cfg.PINE_URL));
        prismLinks.add(prism);
        prismLinks.add(pine);
        add(prismLinks);
        add(Box.createVerticalGlue());
    }

    public void refresh() {
        if (host.prism() == null) {
            status.setText("Prism Launcher не найден — игра не запустится без него.");
            status.setForeground(Palette.WARN);
        }
    }

    public void refreshStatus(UpdateCheck.Info info) {
        if (info == null) {
            status.setText("Проверка не удалась: " + UpdateCheck.lastError());
            status.setForeground(Palette.ERR);
        } else if (UpdateCheck.isNewer(info)) {
            status.setText("Доступна версия " + info.version + " — открой «Релизы» и скачай новый BerryUp.jar.");
            status.setForeground(Palette.ACCENT);
        } else {
            status.setText("Установлена последняя версия: " + Cfg.VERSION);
            status.setForeground(Palette.OK);
        }
    }
}
