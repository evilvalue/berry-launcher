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
        JLabel sub = Widgets.label(Cfg.BUILD + "  ·  Minecraft " + Cfg.MC_VERSION,
                Palette.reg(9f), Palette.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        headText.add(sub);
        headText.add(Box.createVerticalStrut(10));
        head.add(headText, BorderLayout.CENTER);
        add(head);
        add(Box.createVerticalStrut(14));

        Widgets.Card actions = new Widgets.Card();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.pad(18, 20, 18, 20);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        status = Widgets.label("", Palette.reg(8.8f), Palette.DIM);
        status.setAlignmentX(LEFT_ALIGNMENT);
        status.setVisible(false);
        actions.add(status);
        actions.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        Widgets.BerryButton check = new Widgets.BerryButton("Проверить обновление", Widgets.BerryButton.Kind.PRIMARY);
        check.addActionListener(e -> {
            status.setText("Проверяю…");
            status.setVisible(true);
            host.checkUpdates(true);
        });
        Widgets.BerryButton site = new Widgets.BerryButton("Сайт", Widgets.BerryButton.Kind.SECONDARY);
        site.addActionListener(e -> Sys.openUrl(Cfg.SITE));
        Widgets.BerryButton telegram = new Widgets.BerryButton("Telegram", Widgets.BerryButton.Kind.SECONDARY);
        telegram.addActionListener(e -> Sys.openUrl(Cfg.TELEGRAM));
        Widgets.BerryButton releases = new Widgets.BerryButton("Релизы", Widgets.BerryButton.Kind.GHOST);
        releases.addActionListener(e -> Sys.openUrl(Cfg.RELEASES));
        buttons.add(check);
        buttons.add(site);
        buttons.add(telegram);
        buttons.add(releases);
        actions.add(buttons);
        actions.add(Box.createVerticalStrut(6));

        JPanel buttons2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons2.setOpaque(false);
        buttons2.setAlignmentX(LEFT_ALIGNMENT);
        buttons2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        Widgets.BerryButton sources = new Widgets.BerryButton("Исходники", Widgets.BerryButton.Kind.GHOST);
        sources.addActionListener(e -> Sys.openUrl(Cfg.REPO));
        Widgets.BerryButton config = new Widgets.BerryButton("Настройки", Widgets.BerryButton.Kind.GHOST);
        config.addActionListener(e -> Sys.openFolder(Cfg.configDir()));
        Widgets.BerryButton prism = new Widgets.BerryButton("Prism Launcher", Widgets.BerryButton.Kind.GHOST);
        prism.addActionListener(e -> Sys.openUrl(Cfg.PRISM_URL));
        buttons2.add(sources);
        buttons2.add(config);
        buttons2.add(prism);
        actions.add(buttons2);
        add(actions);
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
            status.setVisible(true);
        }
    }

    public void refreshStatus(UpdateCheck.Info info) {
        if (info == null) {
            status.setText("Проверка не удалась: " + UpdateCheck.lastError());
            status.setForeground(Palette.ERR);
            status.setVisible(true);
        } else if (UpdateCheck.isNewer(info)) {
            status.setText("Доступна версия " + info.version + " — открой «Релизы» и скачай новый BerryUp.jar.");
            status.setForeground(Palette.ACCENT);
            status.setVisible(true);
        } else {
            status.setText("Установлена последняя версия: " + Cfg.VERSION);
            status.setForeground(Palette.OK);
            status.setVisible(true);
        }
    }
}
