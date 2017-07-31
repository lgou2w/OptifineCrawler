/*
 * Copyright (C) 2017 The MoonLake Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.minecraft.moonlake.optifinecrawler.gui

import com.minecraft.moonlake.optifinecrawler.OptifineCrawler
import com.minecraft.moonlake.optifinecrawler.OptifineVersion
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.util.Callback
import java.util.concurrent.Callable

class OptifineCrawlerGui: Application() {

    /**************************************************************************
     *
     * Static
     *
     **************************************************************************/

    companion object {
        fun launch(args: Array<out String>) {
            launch(OptifineCrawlerGui::class.java, *args)
        }
    }

    /**************************************************************************
     *
     * Member
     *
     **************************************************************************/

    val version = "1.0.0"
    val author = "Month_Light"
    val title = "OptifineCrawler $version by $author - https://github.com/lgou2w/OptifineCrawler"

    val optifineCrawler = OptifineCrawler()

    /**************************************************************************
     *
     * Implement Method
     *
     **************************************************************************/

    override fun start(primaryStage: Stage) {
        primaryStage.scene = Scene(createUserGui(), 650.0, 400.0)
        primaryStage.isResizable = false
        primaryStage.title = title
        primaryStage.centerOnScreen()
        primaryStage.show()
    }

    /**************************************************************************
     *
     * Private Member
     *
     **************************************************************************/

    private val tableView = TableView<OptifineVersion>()
    private val tableDownload = TableColumn<OptifineVersion, String>("Download")
    private val tablePreview = TableColumn<OptifineVersion, Boolean>("Preview")
    private val tableVersion = TableColumn<OptifineVersion, String>("Version")
    private val tableDate = TableColumn<OptifineVersion, String>("Date")
    private val requestVerList = Button("Get Version List")
    private val rootPane = BorderPane()

    /**************************************************************************
     *
     * Private Method
     *
     **************************************************************************/

    private fun createUserGui(): Parent {
        val btnGroup = HBox(10.0, requestVerList)
        btnGroup.padding = Insets(10.0, .0, 10.0, 10.0)
        tableDownload.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.downloadMirror }) }
        tablePreview.cellValueFactory = Callback { it -> Bindings.createBooleanBinding(Callable { it.value.preview }) }
        tableVersion.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.version }) }
        tableDate.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.date }) }
        tableView.columns.addAll(tableVersion, tableDate, tableDownload, tablePreview)
        rootPane.center = tableView
        rootPane.bottom = btnGroup
        initListener()
        return rootPane
    }

    private fun initListener() {
        requestVerList.setOnAction { _ -> run {
            val verList = optifineCrawler.requestVersionList()
            tableView.items.setAll(verList)
        }}
    }
}
