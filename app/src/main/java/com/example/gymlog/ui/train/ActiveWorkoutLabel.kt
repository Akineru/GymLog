package com.example.gymlog.ui.train

import com.example.gymlog.data.displayName
import com.example.gymlog.ui.session.DraftExerciseUi

/**
 * The exercise the mini bar points at: the first one in the draft that has not
 * been finished yet.
 *
 * Exercises are kept in body-part order, so finishing one moves the bar on to
 * whatever comes next. Returns null when there is nothing left to work on (no
 * exercise added yet, or every exercise already finished); the caller then
 * shows a hint telling the user to add a new exercise.
 *
 * "Finished" means the user tapped 完成动作 (completedAtMillis set). Ticking
 * every set without tapping the button leaves an exercise current, matching the
 * green collapsed-card rule in the editor.
 */
fun currentExerciseLabel(exercises: List<DraftExerciseUi>): String? =
    exercises.firstOrNull { it.completedAtMillis == null }?.exercise?.displayName()
