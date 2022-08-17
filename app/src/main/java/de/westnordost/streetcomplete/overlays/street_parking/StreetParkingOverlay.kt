package de.westnordost.streetcomplete.overlays.street_parking

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.CAR
import de.westnordost.streetcomplete.osm.ALL_ROADS
import de.westnordost.streetcomplete.osm.MAXSPEED_TYPE_KEYS
import de.westnordost.streetcomplete.osm.isPrivateOnFoot
import de.westnordost.streetcomplete.osm.street_parking.IncompleteStreetParking
import de.westnordost.streetcomplete.osm.street_parking.NoStreetParking
import de.westnordost.streetcomplete.osm.street_parking.ParkingPosition.*
import de.westnordost.streetcomplete.osm.street_parking.StreetParking
import de.westnordost.streetcomplete.osm.street_parking.StreetParkingPositionAndOrientation
import de.westnordost.streetcomplete.osm.street_parking.StreetParkingProhibited
import de.westnordost.streetcomplete.osm.street_parking.StreetParkingSeparate
import de.westnordost.streetcomplete.osm.street_parking.StreetStandingProhibited
import de.westnordost.streetcomplete.osm.street_parking.StreetStoppingProhibited
import de.westnordost.streetcomplete.osm.street_parking.UnknownStreetParking
import de.westnordost.streetcomplete.osm.street_parking.createStreetParkingSides
import de.westnordost.streetcomplete.overlays.Color
import de.westnordost.streetcomplete.overlays.Overlay
import de.westnordost.streetcomplete.overlays.PolygonStyle
import de.westnordost.streetcomplete.overlays.PolylineStyle
import de.westnordost.streetcomplete.overlays.StrokeStyle
import de.westnordost.streetcomplete.overlays.Style

class StreetParkingOverlay : Overlay {

    override val title = R.string.overlay_street_parking
    override val icon = R.drawable.ic_quest_parking_lane
    override val changesetComment = "Specify whether there is street parking and what kind"
    override val wikiLink: String = "Key:parking:lane"
    override val achievements = listOf(CAR)

    override fun getStyledElements(mapData: MapDataWithGeometry): Sequence<Pair<Element, Style>> =
        // roads
        mapData.filter("""
            ways with highway ~ trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential|living_street|pedestrian|service
            and area != yes
        """).map { it to getStreetParkingStyle(it) } +
        // separate parking
        mapData.filter(
            "ways with amenity = parking"
        ).map { it to parkingLotStyle } /*+
        TODO: needs icons(?)
        // chokers
        mapData.filter(
            "nodes with traffic_calming ~ choker|chicane|island|choked_island|choked_table"
        ).map { it to chokerStyle }
*/
    override fun createForm(element: Element) =
        if (element.tags["highway"] in ALL_ROADS && element.tags["area"] != "yes")
            StreetParkingOverlayForm()
        else null
}

private val streetParkingTaggingNotExpected by lazy { """
    ways with
      highway ~ service|pedestrian
      or motorroad = yes
      or expressway = yes
      or junction = roundabout
      or tunnel and tunnel != no
      or bridge and bridge != no
      or ~${(MAXSPEED_TYPE_KEYS + "maxspeed").joinToString("|")} ~ .*rural.*
      or maxspeed >= 70
""".toElementFilterExpression() }


private val parkingLotStyle = PolygonStyle(Color.BLUE)

private fun getStreetParkingStyle(element: Element): Style {
    val parking = createStreetParkingSides(element.tags)
    // not set but private or not expected to have a sidewalk -> do not highlight as missing
    if (parking == null) {
        if (isPrivateOnFoot(element) || streetParkingTaggingNotExpected.matches(element)) {
            return PolylineStyle(StrokeStyle(Color.INVISIBLE))
        }
    }

    return PolylineStyle(
        stroke = null,
        strokeLeft = parking?.left.style,
        strokeRight = parking?.right.style
    )
}

private val StreetParking?.style: StrokeStyle get() = when (this) {
    is StreetParkingPositionAndOrientation -> when (position) {
        ON_STREET ->         StrokeStyle(Color.GOLD, dashed = false)
        PAINTED_AREA_ONLY -> StrokeStyle(Color.GOLD, dashed = true)
        HALF_ON_KERB ->      StrokeStyle(Color.AQUAMARINE)
        ON_KERB ->           StrokeStyle(Color.BLUE, dashed = false)
        STREET_SIDE ->       StrokeStyle(Color.BLUE, dashed = true)
    }
    NoStreetParking ->          StrokeStyle(Color.BLACK, dashed = true)
    StreetStandingProhibited,
    StreetParkingProhibited,
    StreetStoppingProhibited -> StrokeStyle(Color.BLACK)
    StreetParkingSeparate ->    StrokeStyle(Color.INVISIBLE)
    UnknownStreetParking ->     StrokeStyle(Color.CRIMSON, dashed = true)
    IncompleteStreetParking,
    null ->                     StrokeStyle(Color.CRIMSON)
}
