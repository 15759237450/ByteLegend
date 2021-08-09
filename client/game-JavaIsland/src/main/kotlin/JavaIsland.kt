import com.bytelegend.app.client.api.GameObjectContainer
import com.bytelegend.app.client.api.GameRuntime
import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.client.api.GameScriptHelpers
import com.bytelegend.app.client.api.HERO_ID
import com.bytelegend.app.client.api.ScriptsBuilder
import com.bytelegend.app.shared.COFFEE
import com.bytelegend.app.shared.Direction
import com.bytelegend.app.shared.GridCoordinate
import com.bytelegend.app.shared.JAVA_ISLAND
import com.bytelegend.app.shared.JAVA_ISLAND_COMMENT_DUNGEON
import com.bytelegend.app.shared.JAVA_ISLAND_DEBUGGER_DUNGEON
import com.bytelegend.app.shared.JAVA_ISLAND_NEWBIE_VILLAGE_PUB
import kotlinx.browser.window

const val BEGINNER_GUIDE_FINISHED_STATE = "BeginnerGuideFinished"
const val NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE = "OldManGotCoffee"
const val NEWBIE_VILLAGE_NOTICEBOARD_MISSION_ID = "remember-brave-people-challenge"
const val STAR_BYTELEGEND_MISSION_ID = "star-bytelegend"
const val STAR_BYTELEGEND_CHALLENGE_ID = "star-bytelegend-challenge"

val gameRuntime = window.asDynamic().gameRuntime.unsafeCast<GameRuntime>()

fun main() {
    gameRuntime.sceneContainer.getSceneById(JAVA_ISLAND).apply {
        objects {
            mapEntrance {
                destMapId = JAVA_ISLAND_NEWBIE_VILLAGE_PUB
            }

            mapEntrance {
                destMapId = JAVA_ISLAND_COMMENT_DUNGEON
            }

            mapEntrance {
                destMapId = JAVA_ISLAND_DEBUGGER_DUNGEON
            }

            pubGuard()
            newbieVillageOldMan()
            newbieVillageHead()
            newbieVillageSailor()
            newbieVillageBridgeSoldier()
            invitationBox()
        }
    }
}

fun GameScene.pubGuard() = objects {
    val helpers = GameScriptHelpers(this@pubGuard)

    npc {
        val guardId = "JavaIslandNewbieVillagePubGuard"
        val guardStartPoint = objects.getPointById("JavaNewbieVillagePubEntranceGuard-point")
        val guardMoveDestPoint = objects.getPointById("JavaNewbieVillagePubEntranceGuard-destination")
        id = guardId
        sprite = "JavaIslandNewbieVillagePubGuard-sprite"
        onInit = {
            when {
                !gameRuntime.heroPlayer.isAnonymous && !gameRuntime.heroPlayer.states.containsKey(BEGINNER_GUIDE_FINISHED_STATE) -> {
                    helpers.getCharacter(guardId).gridCoordinate = guardStartPoint
                    scripts {
                        speech(guardId, "DoYouPreferToBeMediocre")
                        characterMove(HERO_ID, guardStartPoint + GridCoordinate(0, 1)) {
                            helpers.getCharacter(HERO_ID).direction = Direction.UP
                        }
                        talkAboutFirstStar(guardId, objects)
                        startBeginnerGuide()
                        putState(BEGINNER_GUIDE_FINISHED_STATE)
                    }
                }
                playerChallenges.challengeAccomplished(STAR_BYTELEGEND_CHALLENGE_ID) -> {
                    helpers.getCharacter(guardId).gridCoordinate = guardMoveDestPoint
                }
                else -> {
                    helpers.getCharacter(guardId).gridCoordinate = guardStartPoint
                }
            }
        }

        onClick = helpers.standardNpcSpeech(guardId) {
            if (helpers.getCharacter(guardId).gridCoordinate == guardStartPoint) {
                when {
                    !gameRuntime.heroPlayer.states.containsKey(BEGINNER_GUIDE_FINISHED_STATE) && playerChallenges.challengeAccomplished(STAR_BYTELEGEND_CHALLENGE_ID) -> {
                        // Player star first but hasn't finished beginner guide, show them
                        scripts {
                            startBeginnerGuide()
                            putState(BEGINNER_GUIDE_FINISHED_STATE)
                            speech(guardId, "NiceJob", arrayOf("1", "0"))
                            characterMove(guardId, guardMoveDestPoint)
                        }
                    }
                    !gameRuntime.heroPlayer.states.containsKey(BEGINNER_GUIDE_FINISHED_STATE) -> {
                        scripts {
                            talkAboutFirstStar(guardId, objects)
                            startBeginnerGuide()
                            putState(BEGINNER_GUIDE_FINISHED_STATE)
                        }
                    }
                    playerChallenges.challengeAccomplished(STAR_BYTELEGEND_CHALLENGE_ID) -> {
                        // mission accomplished, let's celebrate!
                        scripts {
                            speech(guardId, "NiceJob", arrayOf("1", "0"))
                            characterMove(guardId, guardMoveDestPoint)
                        }
                    }
                    else -> {
                        scripts {
                            talkAboutFirstStar(guardId, objects)
                            startBeginnerGuide()
                        }
                    }
                }
            } else {
                scripts {
                    speech(guardId, "NiceDayHuh", arrow = false)
                }
            }
        }
    }
}

fun GameScene.newbieVillageOldMan() = objects {
    val helpers = GameScriptHelpers(this@newbieVillageOldMan)

    npc {
        val oldManId = "JavaIslandNewbieVillageOldMan"
        val oldManStartPoint = objects.getPointById("$oldManId-point")
        val oldManDestination = objects.getPointById("$oldManId-destination")
        id = oldManId
        sprite = "$oldManId-sprite"
        onInit = {
            if (gameRuntime.heroPlayer.states.containsKey(NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE)) {
                helpers.getCharacter(oldManId).gridCoordinate = oldManDestination
            } else {
                helpers.getCharacter(oldManId).gridCoordinate = oldManStartPoint
            }
        }
        onClick = helpers.standardNpcSpeech(oldManId) {
            when {
                gameRuntime.heroPlayer.states.containsKey(NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE) -> {
                    scripts {
                        speech(oldManId, "NiceDayHuh", arrow = false)
                    }
                }
                gameRuntime.heroPlayer.items.contains(COFFEE) -> {
                    scripts {
                        speech(oldManId, "ThankYouForYourCoffee")
                        putState(NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE)
                        // TODO atomic operation
                        removeItem(COFFEE, oldManStartPoint)
                        characterMove(oldManId, oldManDestination) {
                            helpers.getCharacter(oldManId).direction = Direction.DOWN
                        }
                    }
                }
                else -> {
                    scripts {
                        speech(oldManId, "CanYouPleaseGrabACoffee", arrow = false)
                    }
                }
            }
        }
    }
}

fun GameScene.newbieVillageHead() = objects {
    val helpers = GameScriptHelpers(this@newbieVillageHead)
    npc {
        val villageHeadId = "JavaIslandNewbieVillageHead"
        val startPoint = objects.getPointById("$villageHeadId-point")
        val destPoint = objects.getPointById("$villageHeadId-destination")
        id = villageHeadId
        sprite = "$villageHeadId-sprite"

        onInit = {
            if (playerChallenges.challengeAccomplished(NEWBIE_VILLAGE_NOTICEBOARD_MISSION_ID)) {
                helpers.getCharacter(villageHeadId).gridCoordinate = destPoint
            } else {
                helpers.getCharacter(villageHeadId).gridCoordinate = startPoint
            }
        }

        onClick = helpers.standardNpcSpeech(villageHeadId) {
            if (helpers.getCharacter(villageHeadId).gridCoordinate == startPoint) {
                if (playerChallenges.challengeAccomplished(NEWBIE_VILLAGE_NOTICEBOARD_MISSION_ID)) {
                    scripts {
                        speech(villageHeadId, "OutsideWorldIsDangerousButIHaveToLetYouGo")
                        speech(villageHeadId, "GoodLuckPursueHolyJavaCoffee", arrow = false)
                        characterMove(villageHeadId, destPoint) {
                            helpers.getCharacter(villageHeadId).direction = Direction.DOWN
                        }
                    }
                } else {
                    val noticeboardPoint = objects.getPointById(NEWBIE_VILLAGE_NOTICEBOARD_MISSION_ID).toHumanReadableCoordinate().toString()
                    val javaCastlePoint = objects.getPointById("JavaCastleDoor").toHumanReadableCoordinate().toString()

                    scripts {
                        speech(villageHeadId, "OutsideWorldIsDangerous")
                        speech(HERO_ID, "ButIHaveToDoSomething")
                        speech(villageHeadId, "YouCanFindHolyJavaCoffee", args = arrayOf(javaCastlePoint))
                        speech(villageHeadId, "HolyJavaCoffeeIsAntidote")
                        speech(villageHeadId, "ButYouHaveToGoThroughJavaIsland")
                        speech(HERO_ID, "WithItICanDate")
                        speech(villageHeadId, "LeaveYourName", args = arrayOf(noticeboardPoint), arrow = false)
                    }
                }
            } else {
                scripts {
                    speech(villageHeadId, "GoodLuckPursueHolyJavaCoffee", arrow = false)
                }
            }
        }
    }
}

fun GameScene.newbieVillageSailor() = objects {
    val helpers = GameScriptHelpers(this@newbieVillageSailor)
    npc {
        val sailorId = "JavaIslandNewbieVillageSailor"
        val startPoint = objects.getPointById("$sailorId-point")
        id = sailorId
        sprite = "$sailorId-sprite"

        onInit = {
            helpers.getCharacter(sailorId).gridCoordinate = startPoint
        }

        onClick = helpers.standardNpcSpeech(
            sailorId,
            {
                scripts {
                    speech(sailorId, "ImSupposedToTakeYouToGitIsland", arrow = false)
                }
            }
        ) {
            scripts {
                speech(sailorId, "ImSupposedToTakeYouToGitIsland", arrow = false)
            }
        }
    }
}

fun GameScene.invitationBox() {}
fun GameScene.newbieVillageBridgeSoldier() = objects {
    val helpers = GameScriptHelpers(this@newbieVillageBridgeSoldier)
    npc {
        val soldierId = "JavaIslandNewbieVillageBridgeSoldier"
        val startPoint = objects.getPointById("$soldierId-point")
        val destPoint = objects.getPointById("$soldierId-destination")
        id = soldierId
        sprite = "$soldierId-sprite"

        onInit = {
            helpers.getCharacter(soldierId).gridCoordinate = startPoint
        }

        onClick = helpers.standardNpcSpeech(
            soldierId,
            {
                scripts {
                    speech(soldierId, "ImSupposedToDoSomething", arrow = false)
                }
            }
        ) {
            scripts {
                speech(soldierId, "ImSupposedToDoSomething", arrow = false)
            }
        }
    }
}

fun ScriptsBuilder.talkAboutFirstStar(guardId: String, objects: GameObjectContainer) {
    speech(guardId, "StarCondition", arrayOf("1", "0"))
    speech(HERO_ID, "WhereToFindStar")
    speech(
        guardId, "IDontKnowTakeALookAtStarBytelegend",
        arrayOf(
            objects.getPointById(STAR_BYTELEGEND_MISSION_ID).toHumanReadableCoordinate().toString()
        )
    )
}
