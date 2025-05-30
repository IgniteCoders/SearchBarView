package com.ignite.searchbarview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val editText: EditText
    private val cardView: MaterialCardView
    private val iconSearch: ImageView
    private val iconClear: ImageView
    private val touchFeedback: View
    private val contentLayout: LinearLayout

    // Interfaces para compatibilidad con código existente
    private var onQueryTextChangeListener: OnQueryTextChangeListener? = null
    private var onQueryTextSubmitListener: OnQueryTextSubmitListener? = null

    // Funciones lambda para un uso más moderno
    private var onQueryTextChange: ((newText: String) -> Unit)? = null
    private var onQueryTextSubmit: ((query: String) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_search_bar, this, true)

        editText = findViewById(R.id.editTextSearch)
        iconSearch = findViewById(R.id.iconSearch)
        cardView = findViewById(R.id.cardView)
        iconClear = findViewById(R.id.iconClear)
        touchFeedback = findViewById(R.id.touchFeedback)
        contentLayout = findViewById(R.id.contentLayout)

        setupListeners()
        initializeAttributes(attrs)
    }

    private fun setupListeners() {
        // Configurar el EditText para propagar el evento de touch al touchFeedback
        editText.setOnTouchListener { view, event ->
            // Solo propagamos el evento si el EditText no tiene foco
            if (!editText.hasFocus()) {
                createTransformedTouchEvent(view, event)?.let { transformedEvent ->
                    touchFeedback.dispatchTouchEvent(transformedEvent)
                }
            }
            // Permitir que el EditText también procese el evento
            false
        }

        editText.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            iconClear.isVisible = query.isNotEmpty()
            onQueryTextChangeListener?.onQueryTextChange(query)
            onQueryTextChange?.invoke(query)
        }

        // Configurar el iconClear para propagar el evento de touch al touchFeedback
        iconClear.setOnTouchListener { view, event ->
            // Crear un nuevo evento con las coordenadas transformadas
            createTransformedTouchEvent(view, event)?.let { transformedEvent ->
                touchFeedback.dispatchTouchEvent(transformedEvent)
            }
            // Si es un click, realizar la acción de borrar
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            // Permitir que el iconClear también procese el evento
            false
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

    private fun createTransformedTouchEvent(view: View, event: MotionEvent): MotionEvent? {
        // Obtener las coordenadas del view origen en la pantalla
        val viewCoords = IntArray(2)
        view.getLocationOnScreen(viewCoords)

        // Obtener las coordenadas del touchFeedback en la pantalla
        val feedbackCoords = IntArray(2)
        touchFeedback.getLocationOnScreen(feedbackCoords)

        // Calcular la diferencia de coordenadas
        val x = event.rawX - feedbackCoords[0]
        val y = event.rawY - feedbackCoords[1]

        // Crear un nuevo evento con las coordenadas transformadas
        return MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            x,
            y,
            event.metaState
        )
    }

    private fun initializeAttributes(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.SearchBarView) {
            // Basic
            val text = getResourceId(R.styleable.SearchBarView_text, 0)
            if (text != 0) {
                editText.setText(text)
            }
            
            val hint = getResourceId(R.styleable.SearchBarView_hint, com.google.android.material.R.string.abc_search_hint)
            if (hint != 0) {
                editText.setHint(hint)
            }
            
            // Text Appearance
            val textColor = getColor(
                R.styleable.SearchBarView_textColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)
            )
            val hintTextColor = getColor(
                R.styleable.SearchBarView_hintTextColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            )
            editText.setTextColor(textColor)
            editText.setHintTextColor(hintTextColor)
            
            val textAppearance = getResourceId(R.styleable.SearchBarView_textAppearance, 0)
            if (textAppearance != 0) {
                TextViewCompat.setTextAppearance(editText, textAppearance)
            }

            val textSize = getDimension(R.styleable.SearchBarView_textSize, 0f)
            if (textSize > 0) {
                editText.textSize = textSize
            }

            val textStyle = getInt(R.styleable.SearchBarView_textStyle, Typeface.NORMAL)
            editText.setTypeface(editText.typeface, textStyle)

            val fontFamily = getResourceId(R.styleable.SearchBarView_textFont, 0)
            if (fontFamily != 0) {
                editText.typeface = resources.getFont(fontFamily)
            }

            // Icons
            val leadingIconSize = getDimensionPixelSize(
                R.styleable.SearchBarView_leadingIconSize,
                resources.getDimensionPixelSize(R.dimen.search_bar_icon_size_default)
            )
            val leadingIconPadding = getDimensionPixelSize(
                R.styleable.SearchBarView_leadingIconPadding,
                resources.getDimensionPixelSize(R.dimen.search_bar_icon_padding_default)
            )
            iconSearch.updateLayoutParams {
                width = leadingIconSize
                height = leadingIconSize
            }
            iconSearch.setPadding(leadingIconPadding, leadingIconPadding, leadingIconPadding, leadingIconPadding)
            
            val clearIconSize = getDimensionPixelSize(
                R.styleable.SearchBarView_clearIconSize,
                resources.getDimensionPixelSize(R.dimen.search_bar_icon_size_default)
            )
            val clearIconPadding = getDimensionPixelSize(
                R.styleable.SearchBarView_clearIconPadding,
                resources.getDimensionPixelSize(R.dimen.search_bar_icon_padding_default)
            )
            iconClear.updateLayoutParams {
                width = clearIconSize
                height = clearIconSize
            }
            iconClear.setPadding(clearIconPadding, clearIconPadding, clearIconPadding, clearIconPadding)
            
            val iconTint = getColor(
                R.styleable.SearchBarView_iconTint,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            )
            iconSearch.imageTintList = ColorStateList.valueOf(iconTint)
            iconClear.imageTintList = ColorStateList.valueOf(iconTint)

            // Layout
            val height = getDimensionPixelSize(
                R.styleable.SearchBarView_height,
                resources.getDimensionPixelSize(R.dimen.search_bar_height_default)
            )
            val paddingHorizontal = getDimensionPixelSize(
                R.styleable.SearchBarView_paddingHorizontal,
                resources.getDimensionPixelSize(R.dimen.search_bar_padding_horizontal_default)
            )
            val paddingVertical = getDimensionPixelSize(
                R.styleable.SearchBarView_paddingVertical,
                resources.getDimensionPixelSize(R.dimen.search_bar_padding_vertical_default)
            )
            val elevation = getDimension(
                R.styleable.SearchBarView_elevation,
                resources.getDimension(R.dimen.search_bar_elevation_default)
            )

            cardView.updateLayoutParams {
                this.height = height
            }
            contentLayout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
            cardView.elevation = elevation

            val backgroundColor = getColor(
                R.styleable.SearchBarView_backgroundColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, 0)
            )
            val strokeColor = getColor(
                R.styleable.SearchBarView_strokeColor,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, 0)
            )
            val strokeWidth = getDimensionPixelSize(
                R.styleable.SearchBarView_strokeWidth,
                resources.getDimensionPixelSize(R.dimen.search_bar_stroke_width_default)
            )
            val cornerRadius = getDimension(
                R.styleable.SearchBarView_cornerRadius,
                resources.getDimension(R.dimen.search_bar_corner_radius_default)
            )

            cardView.setCardBackgroundColor(backgroundColor)
            cardView.strokeColor = strokeColor
            cardView.strokeWidth = strokeWidth
            cardView.radius = cornerRadius

            // Input configuration
            val maxLines = getInt(R.styleable.SearchBarView_maxLines, Int.MAX_VALUE)
            val maxLength = getInt(R.styleable.SearchBarView_maxLength, -1)
            val inputType = getInt(R.styleable.SearchBarView_inputType, InputType.TYPE_CLASS_TEXT)
            val imeOptions = getInt(R.styleable.SearchBarView_imeOptions, EditorInfo.IME_ACTION_SEARCH)

            editText.maxLines = maxLines
            if (maxLength > 0) {
                editText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
            }
            editText.inputType = inputType
            editText.imeOptions = imeOptions

            // States
            val isEnabled = getBoolean(R.styleable.SearchBarView_enabled, true)
            setEnabled(isEnabled)
        }
    }

    // Public API para modificar atributos

    fun setHint(hint: String) {
        editText.hint = hint
    }

    fun setLeadingIcon(resourceId: Int) {
        iconSearch.setImageResource(resourceId)
    }

    fun setClearIcon(resourceId: Int) {
        iconClear.setImageResource(resourceId)
    }

    fun setLeadingIconSize(size: Int) {
        iconSearch.updateLayoutParams {
            width = size
            height = size
        }
    }

    fun setClearIconSize(size: Int) {
        iconClear.updateLayoutParams {
            width = size
            height = size
        }
    }

    fun setLeadingIconPadding(padding: Int) {
        iconSearch.setPadding(padding, padding, padding, padding)
    }

    fun setClearIconPadding(padding: Int) {
        iconClear.setPadding(padding, padding, padding, padding)
    }

    fun setHeight(height: Int) {
        cardView.updateLayoutParams {
            this.height = height
        }
    }

    fun setPadding(horizontal: Int, vertical: Int) {
        contentLayout.setPadding(horizontal, vertical, horizontal, vertical)
    }

    override fun setElevation(elevation: Float) {
        cardView.elevation = elevation
    }

    fun setCornerRadius(radius: Float) {
        cardView.radius = radius
    }

    override fun setBackgroundColor(color: Int) {
        cardView.setCardBackgroundColor(color)
    }

    fun setStrokeColor(color: Int) {
        cardView.strokeColor = color
    }

    fun setStrokeWidth(width: Int) {
        cardView.strokeWidth = width
    }

    fun setTextColor(color: Int) {
        editText.setTextColor(color)
    }

    fun setHintTextColor(color: Int) {
        editText.setHintTextColor(color)
    }

    fun setIconTint(color: Int) {
        iconSearch.imageTintList = ColorStateList.valueOf(color)
        iconClear.imageTintList = ColorStateList.valueOf(color)
    }

    fun setTextAppearance(resId: Int) {
        TextViewCompat.setTextAppearance(editText, resId)
    }

    fun setTextSize(size: Float) {
        editText.textSize = size
    }

    fun setFontFamily(resId: Int) {
        editText.typeface = resources.getFont(resId)
    }

    fun setMaxLines(maxLines: Int) {
        editText.maxLines = maxLines
    }

    fun setMaxLength(maxLength: Int) {
        editText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    fun setInputType(inputType: Int) {
        editText.inputType = inputType
    }

    fun setImeOptions(imeOptions: Int) {
        editText.imeOptions = imeOptions
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        editText.isEnabled = enabled
        iconSearch.isEnabled = enabled
        iconClear.isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
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
