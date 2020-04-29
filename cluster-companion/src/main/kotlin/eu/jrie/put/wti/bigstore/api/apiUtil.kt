package eu.jrie.put.wti.bigstore.api

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext

val PipelineContext<*, ApplicationCall>.userId: Int
    get() = call.parameters["id"]!!.toInt()
