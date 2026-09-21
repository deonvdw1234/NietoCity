/*
 * NietoCity - Android classic dialogs (budget, evaluation, graphs).
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import za.co.nieto.nietocity.game.BudgetControl
import za.co.nieto.nietocity.game.CurrencyFormat
import za.co.nieto.nietocity.game.EvaluationReport
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.TerrainConfig
import java.util.Random

/** Builders for the classic city dialogs, shared by the status-bar menu. */
object AppDialogs {

    /**
     * The budget dialog: a tax-rate slider (0..20), road/fire/police funding
     * sliders (percent), live read-outs of tax revenue, expenses and cash flow in
     * rand, and a "don't show automatically" checkbox. Apply writes back to the
     * engine.
     */
    fun showBudget(
        context: Context,
        controller: GameController,
        autoShow: Boolean,
        onAutoShowChanged: (Boolean) -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        val city = controller.engine
        var tax: Int
        var road: Double
        var fire: Double
        var police: Double
        synchronized(city) {
            tax = city.cityTax
            road = city.roadPercent
            fire = city.firePercent
            police = city.policePercent
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(context, 16)
            setPadding(p, dp(context, 8), p, dp(context, 8))
        }

        val revenue = readout(context)
        val expenses = readout(context)
        val cashFlow = readout(context)

        val taxLabel = TextView(context)
        val roadLabel = TextView(context)
        val fireLabel = TextView(context)
        val policeLabel = TextView(context)

        fun refresh() {
            val b = BudgetControl.preview(city, tax, road, fire, police)
            taxLabel.text = "Tax rate: $tax%"
            roadLabel.text = "Road funding: ${pct(road)}%"
            fireLabel.text = "Fire funding: ${pct(fire)}%"
            policeLabel.text = "Police funding: ${pct(police)}%"
            revenue.text = "Tax revenue: ${CurrencyFormat.format(b.taxIncome.toLong())}"
            expenses.text = "Expenses: ${CurrencyFormat.format(b.operatingExpenses.toLong())}"
            val flow = b.taxIncome - b.operatingExpenses
            cashFlow.text = "Cash flow: ${CurrencyFormat.format(flow.toLong())}"
        }

        root.addView(taxLabel)
        root.addView(slider(context, tax, 20) { v -> tax = v; refresh() })
        root.addView(roadLabel)
        root.addView(slider(context, pct(road), 100) { v -> road = v / 100.0; refresh() })
        root.addView(fireLabel)
        root.addView(slider(context, pct(fire), 100) { v -> fire = v / 100.0; refresh() })
        root.addView(policeLabel)
        root.addView(slider(context, pct(police), 100) { v -> police = v / 100.0; refresh() })

        root.addView(spacer(context))
        root.addView(revenue)
        root.addView(expenses)
        root.addView(cashFlow)

        val dontShow = CheckBox(context).apply {
            text = "Don't show automatically"
            isChecked = !autoShow
        }
        root.addView(dontShow)
        refresh()

        val scroll = ScrollView(context).apply { addView(root) }

        AlertDialog.Builder(context)
            .setTitle("City Budget")
            .setView(scroll)
            .setPositiveButton("Apply") { _, _ ->
                BudgetControl.apply(city, tax, road, fire, police)
                onAutoShowChanged(!dontShow.isChecked)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Cancel still honours the checkbox choice.
                onAutoShowChanged(!dontShow.isChecked)
            }
            .setOnDismissListener { onDismiss?.invoke() }
            .show()
    }

    /** The evaluation dialog: approval, score, population, class and top problems.
     *  Read-only; closes on OK, Back or tap-outside. */
    fun showEvaluation(context: Context, controller: GameController) {
        val r = EvaluationReport.of(controller.engine)
        val sb = StringBuilder()
        sb.append("Is the mayor doing a good job?\n")
        sb.append("  Yes: ${r.approveYes}%    No: ${r.approveNo}%\n\n")
        sb.append("City score: ${r.score} (${signed(r.scoreDelta)})\n")
        sb.append("Population: ${r.population} (${signed(r.populationDelta)})\n")
        sb.append("Class: ${r.cityClass}\n\n")
        sb.append("What are the worst problems?\n")
        if (r.problems.isEmpty()) {
            sb.append("  (none reported)")
        } else {
            for (i in r.problems.indices) {
                sb.append("  ${i + 1}. ${r.problems[i]}")
                if (i < r.problems.size - 1) sb.append('\n')
            }
        }
        titledMessage(context, "City Evaluation", sb.toString()).show()
    }

    private fun signed(v: Int): String = if (v >= 0) "+$v" else v.toString()

    /** The graphs dialog: six history line graphs with a 10-year / 120-year toggle. */
    fun showGraphs(context: Context, controller: GameController) {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val graph = GraphView(context).apply {
            bind(controller)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 260)
            )
        }

        val toggle = Button(context).apply {
            text = "Show 120 years"
            setOnClickListener {
                graph.setLongRange(!graph.isLongRange())
                text = if (graph.isLongRange()) "Show 10 years" else "Show 120 years"
            }
        }

        root.addView(toggle)
        root.addView(graph)

        AlertDialog.Builder(context)
            .setTitle("City Graphs")
            .setView(root)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * The New City screen: difficulty, terrain controls (island, lake, river,
     * trees), a seed field (shows the current seed; type one to reproduce a map)
     * and a Reroll button, plus a live preview thumbnail. Start calls back with
     * the chosen level, seed and terrain.
     */
    fun showNewCity(
        context: Context,
        current: GameController,
        onStart: (Int, Long, TerrainConfig) -> Unit
    ) {
        val cfg0 = current.terrainConfig
        val landscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Difficulty.
        val difficulty = RadioGroup(context).apply { orientation = LinearLayout.HORIZONTAL }
        val diffEasy = radio(context, "Easy")
        val diffMedium = radio(context, "Medium")
        val diffHard = radio(context, "Hard")
        difficulty.addView(diffEasy); difficulty.addView(diffMedium); difficulty.addView(diffHard)
        when (current.gameLevel) {
            GameController.LEVEL_MEDIUM -> diffMedium.isChecked = true
            GameController.LEVEL_HARD -> diffHard.isChecked = true
            else -> diffEasy.isChecked = true
        }

        val island = spinner(context, arrayOf("None", "Seldom", "Always"), cfg0.island.ordinal)
        val lake = spinner(context, LEVEL_OPTIONS, cfg0.lake.ordinal)
        val river = spinner(context, LEVEL_OPTIONS, cfg0.river.ordinal)
        val trees = spinner(context, LEVEL_OPTIONS, cfg0.trees.ordinal)

        val seedField = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(current.seed.toString())
        }
        val reroll = Button(context).apply {
            text = "Reroll"
            setOnClickListener { seedField.setText(Random().nextLong().toString()) }
        }

        val preview = za.co.nieto.nietocity.MiniMapPreview(context)

        // Build the form column.
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(context, "Difficulty"))
            addView(difficulty)
            addView(labeledRow(context, "Island", island))
            addView(labeledRow(context, "Lake", lake))
            addView(labeledRow(context, "River", river))
            addView(labeledRow(context, "Trees", trees))
            addView(label(context, "Seed"))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                seedField.layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(seedField)
                addView(reroll)
            })
        }

        fun readConfig(): TerrainConfig = TerrainConfig(
            TerrainConfig.Island.values()[island.selectedItemPosition],
            TerrainConfig.Level.values()[lake.selectedItemPosition],
            TerrainConfig.Level.values()[river.selectedItemPosition],
            TerrainConfig.Level.values()[trees.selectedItemPosition]
        )

        fun readSeed(): Long =
            seedField.text.toString().trim().toLongOrNull() ?: Random().nextLong()

        fun refreshPreview() {
            preview.regenerate(readLevel(diffEasy, diffMedium, diffHard), readSeed(), readConfig())
        }

        // Regenerate the preview whenever a control changes.
        val changed = { refreshPreview() }
        difficulty.setOnCheckedChangeListener { _, _ -> changed() }
        island.onItemSelected(changed); lake.onItemSelected(changed)
        river.onItemSelected(changed); trees.onItemSelected(changed)
        seedField.addTextChangedListener(SimpleWatcher(changed))
        refreshPreview()

        val previewBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(label(context, "Preview"))
            preview.layoutParams = LinearLayout.LayoutParams(dp(context, 160), dp(context, 134))
            addView(preview)
        }

        // Portrait: one scrollable column. Landscape: form beside the preview.
        val content: View = if (landscape) {
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val pad = dp(context, 12)
                setPadding(pad, dp(context, 8), pad, dp(context, 8))
                form.layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(ScrollView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    addView(form)
                })
                addView(previewBox)
            }
        } else {
            ScrollView(context).apply {
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    val pad = dp(context, 16)
                    setPadding(pad, dp(context, 8), pad, dp(context, 8))
                    addView(form)
                    addView(spacer(context))
                    addView(previewBox)
                })
            }
        }

        AlertDialog.Builder(context)
            .setTitle("New City")
            .setView(content)
            .setPositiveButton("Start") { _, _ ->
                onStart(readLevel(diffEasy, diffMedium, diffHard), readSeed(), readConfig())
            }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { preview.dispose() }
            .show()
    }

    /** The Save screen: a name field (defaults to the current name), overwrite confirm. */
    fun showSave(
        context: Context,
        defaultName: String,
        exists: (String) -> Boolean,
        onSave: (String) -> Unit
    ) {
        val field = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(defaultName)
            setSelection(text.length)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(context, 16)
            setPadding(p, dp(context, 8), p, dp(context, 8))
            addView(label(context, "City name"))
            addView(field)
        }
        AlertDialog.Builder(context)
            .setTitle("Save City")
            .setView(root)
            .setPositiveButton("Save") { _, _ ->
                val name = field.text.toString().trim().ifEmpty { defaultName }
                if (exists(name)) {
                    confirm(context, "Overwrite?", "A save named \"$name\" already exists. Overwrite it?",
                        "Overwrite") { onSave(name) }
                } else {
                    onSave(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** The Load screen: a named-slot list with thumbnail, name, date and population. */
    fun showLoad(
        context: Context,
        store: SaveStore,
        onLoad: (SaveSlot) -> Unit
    ) {
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(context).apply { addView(container) }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Load City")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .create()

        fun rebuild() {
            container.removeAllViews()
            val slots = store.list()
            if (slots.isEmpty()) {
                container.addView(label(context, "No saved cities yet.").apply {
                    setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
                })
                return
            }
            for (slot in slots) {
                container.addView(slotRow(context, slot,
                    onClick = { dialog.dismiss(); onLoad(slot) },
                    onDelete = {
                        confirm(context, "Delete save?",
                            "Delete \"${slot.meta.name}\"? This cannot be undone.", "Delete") {
                            store.delete(slot.base)
                            rebuild()
                        }
                    }))
            }
        }
        rebuild()
        dialog.show()
    }

    private fun slotRow(
        context: Context, slot: SaveSlot, onClick: () -> Unit, onDelete: () -> Unit
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))
            isClickable = true
            setOnClickListener { onClick() }
        }
        val thumb = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 72), dp(context, 60))
            if (slot.thumbnail != null) setImageBitmap(slot.thumbnail)
            setBackgroundColor(0xFF000000.toInt())
        }
        row.addView(thumb)
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(context, 10), 0, dp(context, 10), 0)
        }
        textCol.addView(label(context, slot.meta.name).apply { textSize = 16f })
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(slot.meta.dateMillis))
        textCol.addView(label(context, "$date · Pop ${slot.meta.population} · " +
            CurrencyFormat.format(slot.meta.funds.toLong())).apply { textSize = 12f })
        row.addView(textCol)
        row.addView(TextView(context).apply {
            text = "🗑"
            textSize = 18f
            setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6))
            isClickable = true
            setOnClickListener { onDelete() }
        })
        return row
    }

    private fun confirm(
        context: Context, title: String, message: String, positive: String, onYes: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positive) { _, _ -> onYes() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun readLevel(easy: RadioButton, medium: RadioButton, hard: RadioButton): Int =
        when {
            hard.isChecked -> GameController.LEVEL_HARD
            medium.isChecked -> GameController.LEVEL_MEDIUM
            else -> GameController.LEVEL_EASY
        }

    private val LEVEL_OPTIONS = arrayOf("Auto", "None", "Low", "High")

    private fun radio(context: Context, text: String): RadioButton =
        RadioButton(context).apply {
            this.text = text
            setTextColor(0xFFFFFFFF.toInt())
        }

    private fun spinner(context: Context, options: Array<String>, selected: Int): Spinner =
        Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, options)
            setSelection(selected.coerceIn(0, options.size - 1))
        }

    private fun labeledRow(context: Context, text: String, control: View): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lbl = label(context, text)
            lbl.layoutParams = LinearLayout.LayoutParams(dp(context, 64),
                LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(lbl)
            control.layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(control)
        }

    private fun Spinner.onItemSelected(action: () -> Unit) {
        onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) = action()
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    private class SimpleWatcher(val action: () -> Unit) : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) = action()
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
    }

    // --- small view helpers ---

    private fun readout(context: Context): TextView =
        TextView(context).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
        }

    private fun slider(
        context: Context, initial: Int, max: Int, onChange: (Int) -> Unit
    ): SeekBar = SeekBar(context).apply {
        this.max = max
        progress = initial.coerceIn(0, max)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) = onChange(value)
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun spacer(context: Context): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 10)
            )
        }

    internal fun titledMessage(context: Context, title: String, body: CharSequence): AlertDialog =
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton(android.R.string.ok, null)
            .create()

    internal fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()

    internal fun label(context: Context, text: String, gravity: Int = Gravity.START): TextView =
        TextView(context).apply {
            this.text = text
            this.gravity = gravity
            setTextColor(0xFFFFFFFF.toInt())
            ellipsize = TextUtils.TruncateAt.END
        }

    private fun pct(v: Double): Int = Math.round(v * 100).toInt()
}
