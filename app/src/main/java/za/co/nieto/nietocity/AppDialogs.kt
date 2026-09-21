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
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import za.co.nieto.nietocity.game.BudgetControl
import za.co.nieto.nietocity.game.CurrencyFormat
import za.co.nieto.nietocity.game.EvaluationReport
import za.co.nieto.nietocity.game.GameController

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
