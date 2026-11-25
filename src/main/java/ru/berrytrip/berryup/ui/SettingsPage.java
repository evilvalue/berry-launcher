package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Log;
import ru.berrytrip.berryup.Prism;
import ru.berrytrip.berryup.Ram;
import ru.berrytrip.berryup.Store;
import ru.berrytrip.berryup.Sys;
import ru.berrytrip.berryup.Tweaks;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Вкладка «Настройки»: моды, графика, память, поведение лаунчера и журнал. */
public class SettingsPage extends JPanel {

    private final MainFrame host;
    private final JTextArea logArea = new JTextArea();
    private final JScrollPane logScroll;
    private final Widgets.MemorySlider memorySlider;
    private final javax.swing.JLabel memoryValue;
    private final javax.swing.JLabel launcherName;
    private final javax.swing.JLabel launcherPath;
    private final int totalRam;
    private final String[] scaleValues = {"", "1.0", "1.25", "1.5", "1.75", "2.0"};
    private javax.swing.JComboBox<String> scaleBox;
    private boolean scaleSyncing;

    public SettingsPage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BorderLayout());

        Widgets.Page page = new Widgets.Page(new BorderLayout(0, 12));
        page.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 0, 12);

        // ---------------------------------------------------------- моды
        Widgets.Card mods = new Widgets.Card();
        mods.setLayout(new BoxLayout(mods, BoxLayout.Y_AXIS));
        mods.pad(14, 16, 14, 16);
        mods.add(leftAligned(Widgets.caption("КЛИЕНТСКИЕ МОДЫ", Palette.SURFACE)));
        mods.add(Widgets.vgap(2));
        mods.add(leftAligned(Widgets.label("На сервер не влияет — только на твой клиент.",
                Palette.reg(8.5f), Palette.DIM)));
        mods.add(Widgets.vgap(10));

        for (Tweaks.Mod mod : Tweaks.MODS) {
            Widgets.Toggle toggle = new Widgets.Toggle(mod.label, mod.hint, Tweaks.getMod(mod));
            toggle.setAlignmentX(LEFT_ALIGNMENT);
            toggle.setToolTipText(Tweaks.describe(mod));
            toggle.onChange(value -> {
                Tweaks.setMod(mod, value);
                applyTweaks(mod.label + (value ? ": включён" : ": выключен"));
            });
            mods.add(toggle);
            mods.add(Widgets.vgap(2));
        }

        mods.add(Widgets.vgap(6));
        JPanel rule = new JPanel();
        rule.setBackground(Palette.LINE);
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rule.setPreferredSize(new Dimension(10, 1));
        mods.add(rule);
        mods.add(Widgets.vgap(8));
        mods.add(leftAligned(Widgets.caption("ВСЕГДА ВКЛЮЧЕНЫ", Palette.SURFACE)));
        mods.add(Widgets.vgap(4));
        JLabel always = Widgets.label("<html><div style='width:300px'>"
                + Tweaks.lockedText().replace("\n", "<br>") + "</div></html>", Palette.reg(8f), Palette.DIM);
        mods.add(leftAligned(always));
        mods.add(Box.createVerticalGlue());

        mods.setMinimumSize(new Dimension(340, 300));
        gc.gridx = 0;
        gc.weightx = 0.5;
        gc.weighty = 1;
        columns.add(mods, gc);

        // ---------------------------------------------------------- правая колонка
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        Widgets.Card gfx = new Widgets.Card();
        gfx.setLayout(new BoxLayout(gfx, BoxLayout.Y_AXIS));
        gfx.pad(14, 16, 14, 16);
        gfx.add(leftAligned(Widgets.caption("ГРАФИКА", Palette.SURFACE)));
        gfx.add(Widgets.vgap(8));
        Widgets.Toggle packs = new Widgets.Toggle("Ресурспаки", "20 шт. · меньше видеопамяти", Tweaks.getBool("packs", true));
        packs.setAlignmentX(LEFT_ALIGNMENT);
        packs.setToolTipText("Отключает все ресурспаки в options.txt: меньше видеопамяти и быстрее загрузка.");
        packs.onChange(value -> {
            Tweaks.setBool("packs", value);
            applyTweaks(value ? "Ресурспаки включены" : "Ресурспаки выключены");
        });
        gfx.add(packs);
        Widgets.Toggle shaders = new Widgets.Toggle("Шейдеры", "Solas · Iris", Tweaks.getBool("shaders", false));
        shaders.setAlignmentX(LEFT_ALIGNMENT);
        shaders.setToolTipText("Шейдеры Iris. В сборке выключены, чтобы не просаживать FPS.");
        shaders.onChange(value -> {
            Tweaks.setBool("shaders", value);
            applyTweaks(value ? "Шейдеры включены" : "Шейдеры выключены");
        });
        gfx.add(shaders);
        gfx.setMaximumSize(new Dimension(Integer.MAX_VALUE, 172));
        right.add(gfx);
        right.add(Widgets.vgap(12));

        // ---------------------------------------------------------- память
        totalRam = Math.max(8, Ram.gb());
        Widgets.Card memory = new Widgets.Card();
        memory.setLayout(new BoxLayout(memory, BoxLayout.Y_AXIS));
        memory.pad(14, 16, 14, 16);

        JPanel memoryHead = new JPanel(new BorderLayout());
        memoryHead.setOpaque(false);
        memoryHead.setAlignmentX(LEFT_ALIGNMENT);
        memoryHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        memoryHead.add(Widgets.caption("ПАМЯТЬ ДЛЯ ИГРЫ", Palette.SURFACE), BorderLayout.WEST);
        memoryValue = Widgets.label("", Palette.semi(11f), Palette.ACCENT);
        memoryHead.add(memoryValue, BorderLayout.EAST);
        memory.add(memoryHead);
        memory.add(Widgets.vgap(6));

        JPanel sliderRow = new JPanel(new BorderLayout(8, 0));
        sliderRow.setOpaque(false);
        sliderRow.setAlignmentX(LEFT_ALIGNMENT);
        sliderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        Widgets.BerryButton minus = new Widgets.BerryButton("−", Widgets.BerryButton.Kind.SECONDARY);
        minus.setPreferredSize(new Dimension(30, 28));
        Widgets.BerryButton plus = new Widgets.BerryButton("+", Widgets.BerryButton.Kind.SECONDARY);
        plus.setPreferredSize(new Dimension(30, 28));
        memorySlider = new Widgets.MemorySlider(2, totalRam, memoryStart());
        memorySlider.onChange(value -> updateMemoryLabel());
        minus.addActionListener(e -> memorySlider.setValue(memorySlider.value() - 1));
        plus.addActionListener(e -> memorySlider.setValue(memorySlider.value() + 1));
        sliderRow.add(minus, BorderLayout.WEST);
        sliderRow.add(memorySlider, BorderLayout.CENTER);
        sliderRow.add(plus, BorderLayout.EAST);
        memory.add(sliderRow);
        memory.add(Widgets.vgap(8));

        JPanel applyRow = new JPanel(new BorderLayout(8, 0));
        applyRow.setOpaque(false);
        applyRow.setAlignmentX(LEFT_ALIGNMENT);
        applyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        applyRow.add(Widgets.label("всего ОЗУ: " + Ram.gb() + " ГБ", Palette.reg(8f), Palette.DIM), BorderLayout.WEST);
        Widgets.BerryButton apply = new Widgets.BerryButton("Применить", Widgets.BerryButton.Kind.PRIMARY);
        apply.addActionListener(e -> {
            Tweaks.setMemory(memorySlider.value());
            applyTweaks("Память: " + memorySlider.value() + " ГБ");
        });
        applyRow.add(apply, BorderLayout.EAST);
        memory.add(applyRow);
        memory.setMaximumSize(new Dimension(Integer.MAX_VALUE, 172));
        right.add(memory);
        right.add(Widgets.vgap(12));

        // ---------------------------------------------------------- лаунчер
        Widgets.Card behaviour = new Widgets.Card();
        behaviour.setLayout(new BoxLayout(behaviour, BoxLayout.Y_AXIS));
        behaviour.pad(14, 16, 14, 16);
        behaviour.add(leftAligned(Widgets.caption("ЛАУНЧЕР", Palette.SURFACE)));
        behaviour.add(Widgets.vgap(6));

        javax.swing.JPanel pathColumn = new javax.swing.JPanel();
        pathColumn.setOpaque(false);
        pathColumn.setAlignmentX(LEFT_ALIGNMENT);
        pathColumn.setLayout(new BoxLayout(pathColumn, BoxLayout.Y_AXIS));

        launcherName = Widgets.label("Prism Launcher", Palette.reg(8.8f), Palette.FG);
        launcherName.setAlignmentX(LEFT_ALIGNMENT);
        launcherPath = Widgets.label("не найден", Palette.reg(7.6f), Palette.DIM);
        launcherPath.setAlignmentX(LEFT_ALIGNMENT);
        pathColumn.add(launcherName);
        pathColumn.add(Widgets.vgap(2));
        pathColumn.add(launcherPath);
        pathColumn.add(Widgets.vgap(6));

        javax.swing.JPanel pathButtons = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pathButtons.setOpaque(false);
        pathButtons.setAlignmentX(LEFT_ALIGNMENT);
        Widgets.BerryButton pick = new Widgets.BerryButton("Выбрать лаунчер…", Widgets.BerryButton.Kind.SECONDARY);
        pick.setToolTipText("Указать prismlauncher.exe или pineconemc.exe вручную — например, для портативной сборки.");
        pick.addActionListener(e -> host.chooseLauncher());
        Widgets.BerryButton data = new Widgets.BerryButton("Папка данных…", Widgets.BerryButton.Kind.GHOST);
        data.setToolTipText("Папка данных лаунчера, внутри которой лежит instances.");
        data.addActionListener(e -> host.chooseDataDir());
        pathButtons.add(pick);
        pathButtons.add(data);
        pathColumn.add(pathButtons);
        behaviour.add(pathColumn);
        behaviour.add(Widgets.vgap(4));

        Widgets.Toggle autoUpdate = new Widgets.Toggle("Обновления при запуске", "проверять latest.json", Store.autoUpdate());
        autoUpdate.setAlignmentX(LEFT_ALIGNMENT);
        autoUpdate.setToolTipText("Спрашивать launcher/latest.json при каждом старте и предлагать новую версию.");
        autoUpdate.onChange(value -> {
            Store.setAutoUpdate(value);
            Store.save();
            Log.info(value ? "Проверка обновлений при запуске включена." : "Проверка обновлений при запуске выключена.");
        });
        behaviour.add(autoUpdate);
        Widgets.Toggle minimize = new Widgets.Toggle("Сворачивать после игры", "окно уйдёт в панель задач", Store.minimizeOnPlay());
        minimize.setAlignmentX(LEFT_ALIGNMENT);
        minimize.setToolTipText("Окно BerryUp свернётся, как только Prism начнёт запуск игры.");
        minimize.onChange(value -> {
            Store.setMinimizeOnPlay(value);
            Store.save();
        });
        behaviour.add(minimize);

        JPanel shortcutRow = new JPanel(new BorderLayout(8, 0));
        shortcutRow.setOpaque(false);
        shortcutRow.setAlignmentX(LEFT_ALIGNMENT);
        shortcutRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        shortcutRow.add(Widgets.label("рабочий стол и меню «Пуск»", Palette.reg(8f), Palette.DIM),
                BorderLayout.CENTER);
        Widgets.BerryButton shortcut = new Widgets.BerryButton("Создать ярлык", Widgets.BerryButton.Kind.SECONDARY);
        shortcut.setToolTipText("Пересоздать ярлык BerryUp — запуск сразу правильной Java из Prism, без чёрного окна.");
        shortcut.addActionListener(e -> host.makeShortcut());
        shortcutRow.add(shortcut, BorderLayout.EAST);
        behaviour.add(Widgets.vgap(6));
        behaviour.add(shortcutRow);

        JPanel scaleRow = new JPanel(new BorderLayout(8, 0));
        scaleRow.setOpaque(false);
        scaleRow.setAlignmentX(LEFT_ALIGNMENT);
        scaleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        scaleRow.add(Widgets.label("масштаб интерфейса", Palette.reg(8.5f), Palette.FG), BorderLayout.WEST);
        scaleBox = new javax.swing.JComboBox<String>(new String[]{
                "как в системе", "100%", "125%", "150%", "175%", "200%"});
        scaleBox.setFont(Palette.reg(9f));
        scaleBox.setBackground(Palette.SURFACE_ALT);
        scaleBox.setForeground(Palette.FG);
        scaleBox.setFocusable(false);
        String savedScale = Store.get("ui.scale", "");
        for (int i = 0; i < scaleValues.length; i++) {
            if (scaleValues[i].equals(savedScale)) scaleBox.setSelectedIndex(i);
        }
        scaleBox.setToolTipText("Фиксированный масштаб помогает, если на ноутбуке шрифт мелкий или размытый. По умолчанию — как в Windows.");
        scaleBox.addActionListener(e -> {
            if (scaleSyncing) return;
            String value = scaleValues[Math.max(0, scaleBox.getSelectedIndex())];
            if (value.equals(Store.get("ui.scale", ""))) return;
            host.applyUiScale(value);
        });
        scaleRow.add(scaleBox, BorderLayout.EAST);
        behaviour.add(Widgets.vgap(6));
        behaviour.add(scaleRow);
        behaviour.setMaximumSize(new Dimension(Integer.MAX_VALUE, 444));
        right.add(behaviour);
        right.add(Box.createVerticalGlue());

        right.setMinimumSize(new Dimension(340, 300));
        gc.gridx = 1;
        gc.weightx = 0.5;
        gc.insets = new Insets(0, 0, 0, 0);
        columns.add(right, gc);
        page.add(columns, BorderLayout.CENTER);

        // ---------------------------------------------------------- журнал
        Widgets.Card logCard = new Widgets.Card(new BorderLayout());
        logCard.pad(12, 16, 12, 16);
        logCard.setPreferredSize(new Dimension(100, 230));

        JPanel logHead = new JPanel(new BorderLayout());
        logHead.setOpaque(false);
        logHead.add(Widgets.caption("ЖУРНАЛ", Palette.SURFACE), BorderLayout.WEST);
        JPanel logButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        logButtons.setOpaque(false);
        Widgets.BerryButton copy = new Widgets.BerryButton("Копировать", Widgets.BerryButton.Kind.GHOST);
        copy.addActionListener(e -> {
            if (logArea.getText().isEmpty()) {
                Log.info("Журнал пуст.");
            } else if (Sys.copy(logArea.getText())) {
                Log.ok("Журнал скопирован в буфер обмена.");
            } else {
                Log.error("Не удалось скопировать журнал.");
            }
        });
        Widgets.BerryButton open = new Widgets.BerryButton("Открыть", Widgets.BerryButton.Kind.GHOST);
        open.addActionListener(e -> {
            if (Cfg.logPath().isFile()) Sys.openFile(Cfg.logPath());
            else Log.info("Файла журнала ещё нет.");
        });
        Widgets.BerryButton clear = new Widgets.BerryButton("Очистить", Widgets.BerryButton.Kind.GHOST);
        clear.addActionListener(e -> {
            logArea.setText("");
            logArea.setForeground(Palette.MUTED);
            Log.info("Журнал очищен.");
        });
        logButtons.add(copy);
        logButtons.add(open);
        logButtons.add(clear);
        logHead.add(logButtons, BorderLayout.EAST);
        logCard.add(logHead, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(Palette.mono(8.5f));
        logArea.setForeground(Palette.MUTED);
        logArea.setBackground(Palette.SURFACE);
        logArea.setCaretColor(Palette.ACCENT);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 2, 2, 2));
        logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.getVerticalScrollBar().setUnitIncrement(18);
        logCard.add(logScroll, BorderLayout.CENTER);

        page.add(logCard, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        updateMemoryLabel();
        logArea.setText("");
        appendLog("Журнал BerryUp. Файл: " + Log.path());
    }

    /** Вернуть селектор к сохранённому масштабу, если перезапуск отменили. */
    public void refreshScale() {
        if (scaleBox == null) return;
        String saved = Store.get("ui.scale", "");
        scaleSyncing = true;
        for (int i = 0; i < scaleValues.length; i++) {
            if (scaleValues[i].equals(saved)) scaleBox.setSelectedIndex(i);
        }
        scaleSyncing = false;
    }

    /** Показывает, какой лаунчер найден, и обновляется после выбора пути. */
    public void refresh() {
        Prism prism = host.prism();
        if (prism == null) {
            launcherName.setText("Лаунчер не найден");
            launcherName.setForeground(Palette.WARN);
            launcherPath.setText("нажми «Выбрать…» или скачай Prism");
        } else {
            launcherName.setText(prism.kind + (prism.hasInstance() ? "  ·  сборка на месте" : "  ·  сборки нет"));
            launcherName.setForeground(Palette.FG);
            launcherPath.setText(Sys.clipPath(prism.exe.getAbsolutePath(), 52));
        }
        revalidate();
    }

    private int memoryStart() {
        int stored = Tweaks.memory();
        if (stored > 0) return Math.max(2, Math.min(totalRam, stored));
        return totalRam <= 8 ? 4 : (totalRam <= 16 ? 6 : 8);
    }

    private void updateMemoryLabel() {
        memoryValue.setText(memorySlider.value() + " ГБ");
    }

    private void applyTweaks(String message) {
        Tweaks.save();
        Prism prism = host.prism();
        if (prism != null && prism.hasInstance()) {
            Tweaks.applyAll(prism);
        }
        Log.ok(message);
        host.refreshAll();
    }

    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            String current = logArea.getText();
            if (current.length() > 200000) {
                logArea.setText(current.substring(current.length() - 120000));
            }
            logArea.append((logArea.getText().isEmpty() ? "" : "\n") + line);
            JScrollBar bar = logScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private static JPanel leftAligned(java.awt.Component component) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.add(component);
        java.awt.Dimension pref = component.getPreferredSize();
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(1, pref.height)));
        return wrapper;
    }
}
