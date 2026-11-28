package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginsTest {
    @Test
    fun parsesCommands() {
        assertEquals("todo" to "buy milk, call mom", Commands.parse("/todo buy milk, call mom"))
        assertEquals("help" to "", Commands.parse("/help"))
        assertEquals("перевод" to "hi", Commands.parse("/Перевод hi"))
        assertNull(Commands.parse("not a command"))
        assertNull(Commands.parse("/ space"))
    }

    @Test
    fun specFromModelReply() {
        val reply = "sure!\n```json\n{\"name\": \"To Do\", \"description\": \"makes todo lists\", \"instructions\": \"turn the input into a todo list\", \"web\": false}\n```"
        val s = PluginText.parseSpec(reply)!!
        assertEquals("to-do", s.name)
        assertEquals("turn the input into a todo list", s.instructions)
        assertEquals("help-2", PluginText.parseSpec("""{"name":"help","instructions":"x"}""")!!.name)
        assertNull(PluginText.parseSpec("no json here"))
    }

    @Test
    fun kralaCallsPlugins() {
        assertEquals("todo" to "milk, eggs", PluginText.findCall("<plugin todo>milk, eggs</plugin>"))
        assertEquals("", PluginText.stripCall("<plugin todo>milk</plugin>"))
        assertEquals("ok", PluginText.stripCall("ok <plu"))
        assertNull(PluginText.findCall("just text"))
        assertTrue(PluginText.listSection(listOf(PluginSpec("todo", "lists", "x", false))).contains("/todo: lists"))
    }
}
