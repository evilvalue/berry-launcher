package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Log;
import ru.berrytrip.berryup.Prism;
import ru.berrytrip.berryup.Ram;
import ru.berrytrip.berryup.Sys;
import ru.berrytrip.berryup.Tweaks;
import ru.berrytrip.berryup.UpdateCheck;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Вкладка «Диагностика»: что найдено, сколько модов и хватает ли места. */
public class DiagPage extends JPanel {

    private static final String[] KEYS = {"Лаунчер", "Экземпляр", "Java", "Моды", "Сборка", "Память", "Диск"};

    private final MainFrame host;
    private final JLabel[] values = new JLabel[KEYS.length];
    private String[] lastReport = new String[KEYS.length];

    public DiagPage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Widgets.Card card = new Widgets.Card();
        card.setLayout(new GridBagLayout());
        card.pad(16, 16, 16, 16);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 8, 0);
        card.add(Widgets.caption("СОСТОЯНИЕ", Palette.SURFACE), gc);

        for (int i = 0; i < KEYS.length; i++) {
            gc.gridy = i + 1;
            gc.gridwidth = 1;
            gc.insets = new Insets(3, 0, 3, 16);
            gc.fill = GridBagConstraints.NONE;
            gc.weightx = 0;
            card.add(Widgets.label(KEYS[i], Palette.reg(8.6f), Palette.MUTED), gc);

            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            values[i] = Widgets.label("—", Palette.reg(9f), Palette.FG);
            card.add(values[i], gc);
            gc.gridx = 0;
        }
        add(card);
        add(Box.createVerticalStrut(12));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        Widgets.BerryButton refresh = new Widgets.BerryButton("Обновить", Widgets.BerryButton.Kind.SECONDARY);
        refresh.addActionListener(e -> refresh());
        Widgets.BerryButton folder = new Widgets.BerryButton("Открыть папку", Widgets.BerryButton.Kind.SECONDARY);
        folder.addActionListener(e -> host.openInstanceFolder());
        Widgets.BerryButton config = new Widgets.BerryButton("Конфиг сборки", Widgets.BerryButton.Kind.SECONDARY);
        config.addActionListener(e -> {
            Prism prism = host.prism();
            if (prism == null || !prism.hasInstance()) {
                Log.info("Сборка ещё не установлена.");
                return;
            }
            java.io.File file = new java.io.File(prism.instanceDir(), "instance.cfg");
            if (file.isFile()) Sys.openFile(file);
            else Log.error("instance.cfg не найден.");
        });
        Widgets.BerryButton copy = new Widgets.BerryButton("Скопировать отчёт", Widgets.BerryButton.Kind.PRIMARY);
        copy.addActionListener(e -> {
            if (Sys.copy(report())) Log.ok("Отчёт скопирован — его можно отправить в чат.");
            else Log.error("Не удалось скопировать отчёт.");
        });
        buttons.add(refresh);
        buttons.add(folder);
        buttons.add(config);
        buttons.add(copy);
        add(buttons);
        add(Box.createVerticalGlue());

        refresh();
    }

    public void refresh() {
        new Thread(() -> {
            String[] report = buildReport();
            SwingUtilities.invokeLater(() -> {
                lastReport = report;
                for (int i = 0; i < KEYS.length; i++) values[i].setText(report[i]);
            });
        }, "berry-diag").start();
    }

    private String[] buildReport() {
        Prism prism = host.prism();
        String[] report = new String[KEYS.length];

        if (prism == null) {
            report[0] = "не найден — скачай Prism с сайта";
        } else {
            report[0] = prism.kind + " · " + Sys.clipPath(prism.exe.getAbsolutePath(), 64);
        }

        if (prism == null) {
            report[1] = "не установлена";
        } else if (prism.hasInstance()) {
            int mods = prism.modCount();
            report[1] = prism.instanceId() + " · модов: " + mods;
        } else {
            report[1] = "не установлена · искал в " + Sys.clipPath(prism.instancesDir().getAbsolutePath(), 52);
        }

        if (prism == null) {
            report[2] = "не найдена";
        } else {
            java.io.File java = prism.findJava();
            if (java == null) {
                report[2] = "нет Java от Prism — запусти игру один раз";
            } else {
                String version = Prism.javaVersion(java);
                report[2] = (version.isEmpty() ? "Java" : "Java " + version) + " · "
                        + Sys.clipPath(java.getParentFile().getParentFile().getName(), 32);
            }
        }

        report[3] = prism == null || !prism.hasInstance()
                ? "—"
                : prism.modCount() + " включено · " + Tweaks.offCount(prism) + " в berry-off";

        String pack = UpdateCheck.packVersion();
        report[4] = pack.isEmpty() ? "версия не получена" : Cfg.BUILD + " " + pack + " · " + Cfg.LOADER;

        report[5] = Ram.gb() + " ГБ ОЗУ · выделено " + Tweaks.effectiveMemory() + " ГБ";

        java.io.File disk = prism != null ? prism.dataDir : Cfg.ROAMING;
        long free = disk.getUsableSpace() / 1073741824L;
        report[6] = free + " ГБ свободно · " + Sys.clipPath(disk.getAbsolutePath(), 44);
        return report;
    }

    public String report() {
        StringBuilder text = new StringBuilder();
        text.append("BerryUp ").append(Cfg.VERSION).append(" · BerryTrip · ").append(Cfg.LOADER)
                .append(" · Minecraft ").append(Cfg.MC_VERSION).append('\n');
        for (int i = 0; i < KEYS.length; i++) {
            text.append(KEYS[i]).append(": ").append(values[i].getText()).append('\n');
        }
        text.append("настройки: ").append(Cfg.configDir().getAbsolutePath()).append('\n');
        text.append("журнал: ").append(Log.path());
        return text.toString();
    }
}
