package com.bytelegend.client.app.engine

import com.bytelegend.app.client.api.CoordinateAware
import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.client.api.GameScriptHelpers
import com.bytelegend.app.client.api.Sprite
import com.bytelegend.app.shared.GridCoordinate
import com.bytelegend.app.shared.PixelCoordinate
import com.bytelegend.app.shared.objects.GameMapMission
import com.bytelegend.app.shared.objects.GameObject
import com.bytelegend.app.shared.objects.GameObjectRole
import com.bytelegend.client.app.obj.DynamicSprite
import com.bytelegend.client.app.page.game
import com.bytelegend.client.app.ui.mission.MissionModal
import kotlinx.browser.window

/**
 * Represent a mission object in the game
 */
class GameMission(
    override val gameScene: GameScene,
    val gameMapMission: GameMapMission,
    private val sprite: DynamicSprite
) : CoordinateAware, Sprite by sprite {
    override val id: String = gameMapMission.id
    override val gridCoordinate: GridCoordinate = gameMapMission.point
    override val pixelCoordinate: PixelCoordinate = gameMapMission.point * gameScene.map.tileSize
    override val layer: Int = 0
    override val roles: Set<GameObjectRole> =
        setOf(
            GameObjectRole.Mission,
            GameObjectRole.Sprite,
            GameObjectRole.CoordinateAware,
            GameObjectRole.Clickable,
            GameObjectRole.UnableToBeSetAsDestination
        )

    override fun init() {
        gameScene.objects.add(this)
        gameScene.blockers[gridCoordinate.y][gridCoordinate.x]--
    }

    override fun close() {
        gameScene.objects.remove<GameObject>(this.id)
    }

    override fun onClick(): Boolean {
        if (!game.heroPlayer.isAnonymous && gridCoordinate.manhattanDistanceTo(game.hero!!.gridCoordinate) == 1) {
            game.hero!!.direction = GameScriptHelpers(gameScene).faceDirectionOf(game.hero!!, this)
        }
        // If the sprite responds to the click event, delay 500 ms so we can see the effect
        val delayMs = if (sprite.onClick()) 500 else 0
        val defaultGameScene = gameScene.unsafeCast<DefaultGameScene>()
        defaultGameScene.missions.refresh(id)
        window.setTimeout(
            {
                game.modalController.show {
                    attrs.className = "mission-modal"
                    child(MissionModal::class) {
                        attrs.game = game
                        attrs.missionId = id
                        attrs.onClose = {
                            sprite.onMissionModalClosed()
                        }
                    }
                }
            },
            delayMs
        )
        return true
    }
}
