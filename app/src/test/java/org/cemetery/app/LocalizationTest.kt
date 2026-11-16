package org.cemetery.app
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
class LocalizationTest {
    private fun document(locale:String,file:String)=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$locale/$file"))
    private fun names(locale:String,file:String,tag:String):Set<String>{val nodes=document(locale,file).getElementsByTagName(tag);return (0 until nodes.length).map{nodes.item(it).attributes.getNamedItem("name").nodeValue}.toSet()}
    @Test fun allStringsHaveRussianCounterparts(){assertEquals(names("values","strings.xml","string"),names("values-ru","strings.xml","string"))}
    @Test fun allPluralsHaveRussianCounterparts(){assertEquals(names("values","strings.xml","plurals"),names("values-ru","strings.xml","plurals"))}
    @Test fun epitaphIdentifiersAndCountsMatch(){for(locale in listOf("values","values-ru")){val arrays=document(locale,"epitaphs.xml").getElementsByTagName("string-array");assertEquals(3,arrays.length);for(i in 0 until arrays.length){val children=arrays.item(i).childNodes;assertEquals(24,(0 until children.length).count{children.item(it).nodeName=="item"})}}}
    @Test fun russianPluralsCoverGrammar(){val doc=document("values-ru","strings.xml");val plurals=doc.getElementsByTagName("plurals");for(i in 0 until plurals.length){val nodes=plurals.item(i).childNodes;val quantities=(0 until nodes.length).filter{nodes.item(it).nodeName=="item"}.map{nodes.item(it).attributes.getNamedItem("quantity").nodeValue};assertTrue(quantities.containsAll(listOf("one","few","many","other")))}}
}
