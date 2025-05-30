package com.ignite.searchbarview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val editText: EditText
    private val iconSearch: ImageView
    private val cardView: MaterialCardView
    private val iconClear: ImageView
    private val touchFeedback: View

    // Interfaces para compatibilidad con código existente
    private var onQueryTextChangeListener: OnQueryTextChangeListener? = null
    private var onQueryTextSubmitListener: OnQueryTextSubmitListener? = null

    // Funciones lambda para un uso más moderno
    private var onQueryTextChange: ((String) -> Unit)? = null
    private var onQueryTextSubmit: ((String) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_search_bar, this, true)

        editText = findViewById(R.id.editTextSearch)
        iconSearch = findViewById(R.id.iconSearch)
        cardView = findViewById(R.id.cardView)
        iconClear = findViewById(R.id.iconClear)
        touchFeedback = findViewById(R.id.touchFeedback)

        setupListeners()
        initializeAttributes(attrs)
    }

    private fun setupListeners() {
        editText.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            iconClear.isVisible = query.isNotEmpty()
            onQueryTextChangeListener?.onQueryTextChange(query)
            onQueryTextChange?.invoke(query)
        }

        iconClear.setOnClickListener {
            editText.text?.clear()
            onQueryTextChangeListener?.onQueryTextChange("")
            onQueryTextChange?.invoke("")
            iconClear.isVisible = false
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editText.text.toString()
                onQueryTextSubmitListener?.onQueryTextSubmit(query)
                onQueryTextSubmit?.invoke(query)
                true
            } else {
                false
            }
        }

        touchFeedback.setOnClickListener {
            editText.requestFocus()
        }
    }

    private fun initializeAttributes(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.SearchBarView) {
            // Appearance
            var hint = getString(R.styleable.SearchBarView_searchHint)
            val textColor = getColor(
                R.styleable.SearchBarView_searchTextColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)
            )
            val hintTextColor = getColor(
                R.styleable.SearchBarView_searchHintTextColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            )
            val bgColor = getColor(
                R.styleable.SearchBarView_searchBackgroundColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, 0)
            )
            val strokeColor = getColor(
                R.styleable.SearchBarView_searchStrokeColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, 0)
            )
            val strokeWidth = getDimension(R.styleable.SearchBarView_searchStrokeWidth, 0f)
            val iconTint = getColor(
                R.styleable.SearchBarView_searchIconTint,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            )
            val searchIcon = getResourceId(R.styleable.SearchBarView_searchIcon, R.drawable.ic_search)
            val clearIcon = getResourceId(R.styleable.SearchBarView_searchClearIcon, R.drawable.ic_clear)
            val cornerRadius = getDimension(
                R.styleable.SearchBarView_searchCornerRadius,
                resources.getDimension(R.dimen.search_bar_height) / 2
            )

            // Behavior
            val isEnabled = getBoolean(R.styleable.SearchBarView_searchEnabled, true)
            val isSingleLine = getBoolean(R.styleable.SearchBarView_searchSingleLine, true)
            val imeOptions = getInt(R.styleable.SearchBarView_searchImeOptions, EditorInfo.IME_ACTION_SEARCH)

            // Apply attributes
            editText.apply {
                hint = hint ?: context.getString(R.string.search_hint)
                setTextColor(textColor)
                setHintTextColor(hintTextColor)
                this.isEnabled = isEnabled
                maxLines = if (isSingleLine) 1 else Int.MAX_VALUE
                this.imeOptions = imeOptions
            }

            iconSearch.apply {
                setImageResource(searchIcon)
                setColorFilter(iconTint)
            }

            iconClear.apply {
                setImageResource(clearIcon)
                setColorFilter(iconTint)
                isClickable = true
                isFocusable = true
            }

            cardView.apply {
                setCardBackgroundColor(bgColor)
                this.strokeWidth = strokeWidth.toInt()
                this.strokeColor = strokeColor
                radius = cornerRadius
                elevation = 0f
            }
        }
    }

    // Public API

    fun getText(): String = editText.text.toString()

    fun setText(text: String) {
        editText.setText(text)
    }

    // Métodos para establecer listeners usando interfaces
    fun setOnQueryTextChangeListener(listener: OnQueryTextChangeListener) {
        this.onQueryTextChangeListener = listener
    }

    fun setOnQueryTextSubmitListener(listener: OnQueryTextSubmitListener) {
        this.onQueryTextSubmitListener = listener
    }

    // Métodos para establecer listeners usando funciones lambda
    fun setOnQueryTextChangeListener(listener: (newText: String) -> Unit) {
        this.onQueryTextChange = listener
    }

    fun setOnQueryTextSubmitListener(listener: (query: String) -> Unit) {
        this.onQueryTextSubmit = listener
    }

    // Interfaces
    interface OnQueryTextChangeListener {
        fun onQueryTextChange(newText: String)
    }

    interface OnQueryTextSubmitListener {
        fun onQueryTextSubmit(query: String)
    }
}
