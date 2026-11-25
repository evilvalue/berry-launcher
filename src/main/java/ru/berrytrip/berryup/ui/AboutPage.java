package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Sys;
import ru.berrytrip.berryup.UpdateCheck;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
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

        Widgets.Card head = new Widgets.Card(new BorderLayout(18, 0));
        head.pad(22, 24, 22, 24);
        head.setAlignmentX(LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 138));
        head.tint(Palette.ACCENT);
        head.add(new Widgets.Logo(62), BorderLayout.WEST);

        JPanel headText = new JPanel();
        headText.setOpaque(false);
        headText.setLayout(new BoxLayout(headText, BoxLayout.Y_AXIS));
        JLabel title = Widgets.label("BerryUp " + Cfg.VERSION, Palette.semi(25f), Palette.FG);
        title.setAlignmentX(LEFT_ALIGNMENT);
        headText.add(title);
        headText.add(Box.createVerticalStrut(3));
        JLabel sub = Widgets.label("Надстройка над Prism Launcher  ·  " + Cfg.BUILD
                + "  ·  Minecraft " + Cfg.MC_VERSION, Palette.reg(9f), Palette.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        headText.add(sub);
        headText.add(Box.createVerticalStrut(10));
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(LEFT_ALIGNMENT);
        chips.add(new Widgets.Chip("Java 8+", Palette.VIOLET));
        chips.add(new Widgets.Chip("Windows", Palette.MUTED));
        chips.add(new Widgets.Chip("~1,5 МБ", Palette.OK));
        headText.add(chips);
        head.add(headText, BorderLayout.CENTER);
        add(head);
        add(Box.createVerticalStrut(14));

        Widgets.Card notes = new Widgets.Card();
        notes.setLayout(new BoxLayout(notes, BoxLayout.Y_AXIS));
        notes.pad(20, 24, 20, 24);
        notes.setAlignmentX(LEFT_ALIGNMENT);
        notes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        notes.add(left(Widgets.caption("ЧТО НОВОГО В 3.9.1", Palette.SURFACE)));
        notes.add(Box.createVerticalStrut(10));

        JTextArea list = Widgets.paragraph(
                "•  Скин 128×128 теперь ложится на модель без искажений, как в QuickSkin.\n"
                        + "•  Пиксельный Monocraft — шрифт в духе Minecraft, с полной кириллицей.\n"
                        + "•  Тёмная и светлая темы: переключатель в «Настройках», выбор запоминается.\n"
                        + "•  Пиксельная малина вместо логотипа — в лаунчере и на сайте.\n"
                        + "•  Скин Стива по умолчанию в 3D-превью и поворот модели по вертикали вниз.\n"
                        + "•  Плащи QuickSkin, drag&drop и проверка размеров остались на месте.",
                Palette.reg(9.3f), Palette.MUTED);
        list.setAlignmentX(LEFT_ALIGNMENT);
        list.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        notes.add(list);
        notes.add(Box.createVerticalStrut(12));

        status = Widgets.label("Проверка обновлений — по кнопке ниже.", Palette.reg(8.8f), Palette.DIM);
        status.setAlignmentX(LEFT_ALIGNMENT);
        notes.add(status);
        add(notes);
        add(Box.createVerticalStrut(14));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        Widgets.BerryButton check = new Widgets.BerryButton("Проверить обновление", Widgets.BerryButton.Kind.PRIMARY);
        check.addActionListener(e -> {
            status.setText("Проверяю…");
            host.checkUpdates(true);
        });
        Widgets.BerryButton site = new Widgets.BerryButton("Сайт сборки", Widgets.BerryButton.Kind.SECONDARY);
        site.addActionListener(e -> Sys.openUrl(Cfg.SITE));
        Widgets.BerryButton telegram = new Widgets.BerryButton("Telegram", Widgets.BerryButton.Kind.SECONDARY);
        telegram.addActionListener(e -> Sys.openUrl(Cfg.TELEGRAM));
        Widgets.BerryButton releases = new Widgets.BerryButton("Релизы", Widgets.BerryButton.Kind.GHOST);
        releases.addActionListener(e -> Sys.openUrl(Cfg.RELEASES));
        buttons.add(check);
        buttons.add(site);
        buttons.add(telegram);
        buttons.add(releases);
        add(buttons);

        JPanel buttons2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons2.setOpaque(false);
        buttons2.setAlignmentX(LEFT_ALIGNMENT);
        buttons2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        Widgets.BerryButton sources = new Widgets.BerryButton("Исходники", Widgets.BerryButton.Kind.GHOST);
        sources.addActionListener(e -> Sys.openUrl(Cfg.REPO));
        Widgets.BerryButton config = new Widgets.BerryButton("Папка настроек", Widgets.BerryButton.Kind.GHOST);
        config.addActionListener(e -> Sys.openFolder(Cfg.configDir()));
        Widgets.BerryButton prism = new Widgets.BerryButton("Скачать Prism Launcher", Widgets.BerryButton.Kind.SECONDARY);
        prism.addActionListener(e -> Sys.openUrl(Cfg.PRISM_URL));
        Widgets.BerryButton pine = new Widgets.BerryButton("PineconeMC", Widgets.BerryButton.Kind.GHOST);
        pine.addActionListener(e -> Sys.openUrl(Cfg.PINE_URL));
        buttons2.add(sources);
        buttons2.add(config);
        buttons2.add(prism);
        buttons2.add(pine);
        add(buttons2);
        add(Box.createVerticalStrut(10));

        JLabel note = Widgets.label("BerryUp не заменяет Prism Launcher: без него игра не запустится.",
                Palette.reg(8.4f), Palette.DIM);
        note.setAlignmentX(LEFT_ALIGNMENT);
        add(note);
        add(Box.createVerticalGlue());
    }

    private JLabel left(JLabel label) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
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
