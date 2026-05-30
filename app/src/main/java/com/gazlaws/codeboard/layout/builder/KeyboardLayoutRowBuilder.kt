package com.gazlaws.codeboard.layout.builder

import com.gazlaws.codeboard.layout.Box
import com.gazlaws.codeboard.layout.Key

// 키보드 한 행(row)을 구성: 키 정보들을 받아 실제 Key(위치 포함) 목록으로 빌드한다
class KeyboardLayoutRowBuilder {

    private var box: Box? = null // 행의 크기
    private val keys = ArrayList<KeyInfo>()
    private var gap = 0f

    @Throws(KeyboardLayoutException::class)
    fun build(): ArrayList<Key> {
        checkAndUpdateDefaults()
        val box = this.box!!
        if (keys.isEmpty()) {
            throw KeyboardLayoutException("Row cannot be built without any keys")
        }
        val availableWidth = box.width - gap * (keys.size - 1)
        val availableHeight = box.height
        if (availableWidth <= 0) {
            throw KeyboardLayoutException("Not enough space to fit keys in row")
        }
        var totalRequestedSize = 0f
        for (info in keys) {
            totalRequestedSize += info.size
        }
        var cursorX = box.x
        val cursorY = box.y
        val result = ArrayList<Key>()
        for (info in keys) {
            val width = availableWidth / totalRequestedSize * info.size
            val height = availableHeight
            val keyBox = Box.create(cursorX, cursorY, width, height)
            cursorX += keyBox.width + gap
            result.add(buildKeyFromBlueprint(info, keyBox))
        }
        return result
    }

    fun addKey(key: KeyInfo): KeyboardLayoutRowBuilder {
        keys.add(key)
        return this
    }

    fun setBox(size: Box): KeyboardLayoutRowBuilder {
        this.box = size
        return this
    }

    fun setGap(size: Float): KeyboardLayoutRowBuilder {
        this.gap = size
        return this
    }

    // box가 설정되지 않았으면 0 크기 기본값으로 채운다
    private fun checkAndUpdateDefaults() {
        if (box == null) {
            box = Box.create(0f, 0f, 0f, 0f)
        }
    }

    companion object {
        // 키 정보와 박스로 실제 Key 객체를 만든다
        private fun buildKeyFromBlueprint(info: KeyInfo, box: Box): Key {
            val key = Key()
            key.box = box
            key.info = info
            return key
        }
    }
}
