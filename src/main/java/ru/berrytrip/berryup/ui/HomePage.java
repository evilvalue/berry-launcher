package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Prism;
import ru.berrytrip.berryup.Tweaks;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/** Первая вкладка: сборка, короткая сводка, ссылки на сайт и Telegram. */
public class HomePage extends JPanel {

    private final MainFrame host;
    private final Widgets.Card warning;
    private final JTextArea warningText;
    private final Widgets.BerryButton install;
    private final Widgets.BerryButton pickLauncher;
    private final JLabel infoBuild;
    private final JLabel infoServer;
    private final JLabel infoVersion;
    private final JLabel statMods;
    private final JLabel statMemory;
    private final Widgets.Chip heroChip;

    public HomePage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // ---------------------------------------------------------- hero-карточка
        Widgets.Card hero = new Widgets.Card(new BorderLayout(20, 0));
        hero.setAlignmentX(LEFT_ALIGNMENT);
        hero.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        hero.tint(Palette.ACCENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 178));
        hero.add(new Widgets.Logo(66), BorderLayout.WEST);

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        JLabel title = Widgets.label(Cfg.BUILD, Palette.semi(29f), Palette.FG);
        title.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(2));
        JLabel sub = Widgets.label("Minecraft " + Cfg.MC_VERSION + "  ·  " + Cfg.LOADER
                + "  ·  BerryUp " + Cfg.VERSION, Palette.reg(9f), Palette.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(sub);
        heroText.add(Box.createVerticalStrut(10));
        JTextArea intro = Widgets.paragraph(
                "Выживание, приключения и друзья. Запуск и сервер — слева, клиент и скины — во вкладках.",
                Palette.reg(9.4f), Palette.MUTED);
        intro.setAlignmentX(LEFT_ALIGNMENT);
        intro.setRows(2);
        heroText.add(intro);
        heroText.add(Box.createVerticalStrut(10));

        JPanel heroChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        heroChips.setOpaque(false);
        heroChips.setAlignmentX(LEFT_ALIGNMENT);
        heroChip = new Widgets.Chip("", Palette.OK);
        heroChips.add(heroChip);
        heroChips.add(new Widgets.Chip("QuickSkin · скины и плащи", Palette.VIOLET));
        heroText.add(heroChips);
        hero.add(heroText, BorderLayout.CENTER);
        add(hero);
        add(Box.createVerticalStrut(14));

        // ---------------------------------------------------------- предупреждение
        warning = new Widgets.Card(new BorderLayout(14, 0));
        warning.setAlignmentX(LEFT_ALIGNMENT);
        warning.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        warning.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        warning.tint(Palette.WARN);
        warningText = Widgets.paragraph("Сборка ещё не установлена в Prism.", Palette.reg(9.2f), Palette.WARN);
        warningText.setRows(2);
        warning.add(warningText, BorderLayout.CENTER);
        install = new Widgets.BerryButton("Установить сборку", Widgets.BerryButton.Kind.PRIMARY);
        install.addActionListener(e -> {
            if (host.prism() == null) {
                ru.berrytrip.berryup.Sys.openUrl(Cfg.PRISM_URL);
                return;
            }
            host.installInstance();
        });
        JPanel warningButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        warningButtons.setOpaque(false);
        pickLauncher = new Widgets.BerryButton("Указать путь…", Widgets.BerryButton.Kind.SECONDARY);
        pickLauncher.addActionListener(e -> host.chooseLauncher());
        warningButtons.add(pickLauncher);
        warningButtons.add(install);
        warning.add(warningButtons, BorderLayout.EAST);
        add(warning);
        add(Box.createVerticalStrut(14));

        // ---------------------------------------------------------- сводка
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        statMods = statCard(stats, "МОДОВ", Palette.ACCENT);
        statMemory = statCard(stats, "ВЫДЕЛЕНО ПАМЯТИ", Palette.OK);
        statCard(stats, "ВЕРСИЯ BERRYUP", Palette.FG).setText("v" + Cfg.VERSION);
        add(stats);
        add(Box.createVerticalStrut(18));

        // ---------------------------------------------------------- ссылки
        JLabel linksCaption = Widgets.label("ГДЕ СЛЕДИТЬ ЗА СБОРКОЙ", Palette.semi(8.5f), Palette.DIM);
        linksCaption.setAlignmentX(LEFT_ALIGNMENT);
        add(linksCaption);
        add(Box.createVerticalStrut(8));

        JPanel links = new JPanel(new GridLayout(1, 2, 12, 0));
        links.setOpaque(false);
        links.setAlignmentX(LEFT_ALIGNMENT);
        links.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        links.add(new Widgets.LinkCard("Сайт сборки", "evilvalue.github.io/berry", Cfg.SITE, Widgets.Glyph.SITE));
        links.add(new Widgets.LinkCard("Telegram-канал", "@killberries", Cfg.TELEGRAM, Widgets.Glyph.TELEGRAM));
        add(links);
        add(Box.createVerticalStrut(18));

        Widgets.Card info = new Widgets.Card();
        info.setAlignmentX(LEFT_ALIGNMENT);
        info.setLayout(new GridBagLayout());
        info.pad(16, 18, 16, 18);
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 8, 0);
        info.add(Widgets.label("КОРОТКО О СБОРКЕ", Palette.semi(8.5f), Palette.DIM), gc);

        infoBuild = Widgets.label(Cfg.BUILD + "  ·  " + Cfg.LOADER + "  ·  Minecraft " + Cfg.MC_VERSION,
                Palette.reg(9.4f), Palette.FG);
        infoServer = Widgets.label(Cfg.SERVER_HOST + ":" + Cfg.SERVER_PORT, Palette.reg(9.4f), Palette.FG);
        infoVersion = Widgets.label("BerryUp " + Cfg.VERSION, Palette.reg(9.4f), Palette.FG);

        addRow(info, gc, 1, "Сборка", infoBuild);
        addRow(info, gc, 2, "Сервер", infoServer);
        addRow(info, gc, 3, "Версия", infoVersion);
        add(info);
        add(Box.createVerticalStrut(12));

        JLabel hint = Widgets.label("Ссылки откроются в браузере. Журнал и диагностика — в соседних вкладках.",
                Palette.reg(8.2f), Palette.DIM);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        add(hint);
        add(Box.createVerticalGlue());

        refresh();
    }

    /** Маленькая карточка-цифра для сводки. */
    private JLabel statCard(JPanel parent, String caption, java.awt.Color valueColor) {
        Widgets.Card card = new Widgets.Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.pad(16, 18, 14, 18);
        JLabel value = Widgets.label("—", Palette.semi(24f), valueColor);
        value.setAlignmentX(LEFT_ALIGNMENT);
        card.add(value);
        card.add(Box.createVerticalStrut(4));
        JLabel text = Widgets.label(caption, Palette.semi(7.8f), Palette.DIM);
        text.setAlignmentX(LEFT_ALIGNMENT);
        card.add(text);
        parent.add(card);
        return value;
    }

    private void addRow(JPanel card, GridBagConstraints gc, int row, String key, JLabel value) {
        gc.gridy = row;
        gc.gridwidth = 1;
        gc.insets = new Insets(3, 0, 3, 14);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        card.add(Widgets.label(key, Palette.reg(8.8f), Palette.MUTED), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        card.add(value, gc);
        gc.gridx = 0;
    }

    public void refresh() {
        Prism prism = host.prism();
        boolean hasLauncher = prism != null;
        boolean installed = hasLauncher && prism.hasInstance();
        warning.setVisible(!installed);
        if (!installed) {
            warningText.setText(hasLauncher
                    ? "Сборка BerryTrip не установлена в " + prism.kind + "."
                    : "Prism Launcher не найден — BerryUp работает поверх него.");

            install.setEnabled(true);
            install.setText(hasLauncher ? "Установить сборку" : "Скачать Prism");
            pickLauncher.setVisible(!hasLauncher);
        }

        int mods = prism != null && prism.hasInstance() ? prism.modCount() : 0;
        statMods.setText(mods > 0 ? String.valueOf(mods) : "—");
        statMemory.setText(Tweaks.effectiveMemory() + " ГБ");
        if (installed) {
            heroChip.setText("сборка готова к игре · " + mods + " модов");
            heroChip.setColor(Palette.OK);
        } else {
            heroChip.setText("сборка ставится в один клик");
            heroChip.setColor(Palette.WARN);
        }

        revalidate();
        repaint();
    }
}
