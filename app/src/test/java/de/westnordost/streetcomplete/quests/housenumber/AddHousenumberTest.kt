package de.westnordost.streetcomplete.quests.housenumber

import de.westnordost.osmapi.map.data.*
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryAdd
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.geometry.ElementPolygonsGeometry
import de.westnordost.streetcomplete.p
import de.westnordost.streetcomplete.quests.TestMapDataWithGeometry
import de.westnordost.streetcomplete.quests.verifyAnswer
import org.junit.Assert.*
import org.junit.Test

class AddHousenumberTest {

    private val questType = AddHousenumber()

    @Test fun `does not create quest for generic building`() {
        val building = OsmWay(1L, 1, NODES1, mapOf("building" to "yes"))
        val mapData = createMapData(mapOf(building to POSITIONS1))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertEquals(false, questType.isApplicableTo(building))
    }

    @Test fun `does not create quest for building with address`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached",
            "addr:housenumber" to "123"
        ))
        val mapData = createMapData(mapOf(building to POSITIONS1))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertEquals(false, questType.isApplicableTo(building))
    }

    @Test fun `does create quest for building without address`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached"
        ))
        val mapData = createMapData(mapOf(building to POSITIONS1))
        assertEquals(1, questType.getApplicableElements(mapData).toList().size)
        assertNull(questType.isApplicableTo(building))
    }

    @Test fun `does not create quest for building with address node on outline`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached"
        ))
        val addr = OsmNode(2L, 1, P2, mapOf(
            "addr:housenumber" to "123"
        ))
        val mapData = createMapData(mapOf(
            building to POSITIONS1,
            addr to ElementPointGeometry(P2)
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertNull(questType.isApplicableTo(building))
    }

    @Test fun `does not create quest for building that is part of a relation with an address`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached"
        ))
        val relationWithAddr = OsmRelation(2L, 1, listOf(
            OsmRelationMember(1L, "something", Element.Type.WAY)
        ), mapOf(
            "addr:housenumber" to "123"
        ))

        val mapData = createMapData(mapOf(
            building to POSITIONS1,
            relationWithAddr to ElementPointGeometry(P2)
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertNull(questType.isApplicableTo(building))
    }

    @Test fun `does not create quest for building that is inside an area with an address`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached"
        ))
        val areaWithAddr = OsmWay(1L, 1, NODES2, mapOf(
            "addr:housenumber" to "123",
            "amenity" to "school",
        ))
        val mapData = createMapData(mapOf(
            building to POSITIONS1,
            areaWithAddr to POSITIONS2,
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertNull(questType.isApplicableTo(building))
    }

    @Test fun `does not create quest for building that contains an address node`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached"
        ))
        val addr = OsmNode(1L, 1, PC, mapOf(
            "addr:housenumber" to "123"
        ))
        val mapData = createMapData(mapOf(
            building to POSITIONS1,
            addr to ElementPointGeometry(PC),
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertNull(questType.isApplicableTo(building))
    }

    @Test fun `does not create quest for building that intersects bounding box`() {
        val building = OsmWay(1L, 1, NODES1, mapOf(
            "building" to "detached"
        ))
        val mapData = createMapData(mapOf(
            building to ElementPolygonsGeometry(listOf(listOf(P1, P2, PO, P4, P1)), PC)
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
        assertNull(questType.isApplicableTo(building))
    }

    @Test fun `housenumber regex`() {
        val r = VALID_HOUSENUMBER_REGEX
        assertTrue("1".matches(r))
        assertTrue("1234".matches(r))

        assertTrue("1234a".matches(r))
        assertTrue("1234/a".matches(r))
        assertTrue("1234 / a".matches(r))
        assertTrue("1234 / A".matches(r))
        assertTrue("1234A".matches(r))
        assertTrue("1234/9".matches(r))
        assertTrue("1234 / 9".matches(r))

        assertTrue("12345".matches(r))
        assertFalse("123456".matches(r))
        assertFalse("1234 5".matches(r))
        assertFalse("1234/55".matches(r))
        assertFalse("1234AB".matches(r))
    }

    @Test fun `blocknumber regex`() {
        val r = VALID_BLOCKNUMBER_REGEX
        assertTrue("1".matches(r))
        assertTrue("1234".matches(r))
        assertFalse("12345".matches(r))

        assertTrue("1234a".matches(r))
        assertTrue("1234 a".matches(r))
        assertFalse("1234 ab".matches(r))
    }

    @Test fun `apply house number answer`() {
        questType.verifyAnswer(
            HouseNumber("99b"),
            StringMapEntryAdd("addr:housenumber", "99b")
        )
    }

    @Test fun `apply house name answer`() {
        questType.verifyAnswer(
            HouseName("La Escalera"),
            StringMapEntryAdd("addr:housename", "La Escalera")
        )
    }

    @Test fun `apply conscription number answer`() {
        questType.verifyAnswer(
            ConscriptionNumber("I.123"),
            StringMapEntryAdd("addr:conscriptionnumber", "I.123"),
            StringMapEntryAdd("addr:housenumber", "I.123")
        )
    }

    @Test fun `apply conscription and street number answer`() {
        questType.verifyAnswer(
            ConscriptionNumber("I.123", "12b"),
            StringMapEntryAdd("addr:conscriptionnumber", "I.123"),
            StringMapEntryAdd("addr:streetnumber", "12b"),
            StringMapEntryAdd("addr:housenumber", "12b")
        )
    }

    @Test fun `apply block and house number answer`() {
        questType.verifyAnswer(
            HouseAndBlockNumber("12A", "123"),
            StringMapEntryAdd("addr:block_number", "123"),
            StringMapEntryAdd("addr:housenumber", "12A")
        )
    }

    @Test fun `apply no house number answer`() {
        questType.verifyAnswer(
            NoHouseNumber,
            StringMapEntryAdd("nohousenumber", "yes")
        )
    }
}

private val P1 = p(0.25,0.25)
private val P2 = p(0.25,0.75)
private val P3 = p(0.75,0.75)
private val P4 = p(0.75,0.25)

private val P5 = p(0.1,0.1)
private val P6 = p(0.1,0.9)
private val P7 = p(0.9,0.9)
private val P8 = p(0.9,0.1)

private val PO = p(1.5, 1.5)
private val PC = p(0.5,0.5)

private val NODES1 = listOf<Long>(1,2,3,4,1)
private val NODES2 = listOf<Long>(5,6,7,8,5)

private val POSITIONS1 = ElementPolygonsGeometry(listOf(listOf(P1, P2, P3, P4, P1)), PC)
private val POSITIONS2 = ElementPolygonsGeometry(listOf(listOf(P5, P6, P7, P8, P5)), PC)

private fun createMapData(elements: Map<Element, ElementGeometry?>): TestMapDataWithGeometry {
    val result = TestMapDataWithGeometry(elements.keys)
    for((element, geometry) in elements) {
        when(element) {
            is Node ->
                result.nodeGeometriesById[element.id] = geometry as ElementPointGeometry
            is Way ->
                result.wayGeometriesById[element.id] = geometry
            is Relation ->
                result.relationGeometriesById[element.id] = geometry
        }
    }
    result.handle(BoundingBox(0.0, 0.0, 1.0, 1.0))
    return result
}
