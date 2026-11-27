@preconcurrency import shared
import Foundation

extension Mapping {
    static func feedback(_ f: Feedback?) -> FeedbackInfo? {
        guard let f else { return nil }
        return FeedbackInfo(
            serial: Int(f.serial),
            kind: FeedbackKindValue(rawValue: key(f.kind)) ?? .failed,
            text: f.text,
            count: Int(f.count),
            error: error(f.error)
        )
    }

    static func person(_ p: TitlePerson, index: Int) -> TitlePersonInfo {
        TitlePersonInfo(id: "\(index)-\(p.name)", personId: p.id, name: p.name, role: p.role, character: p.character, profile: p.profile)
    }

    static func card(_ c: TitleCard) -> TitleCardInfo {
        TitleCardInfo(
            tmdbId: c.tmdbId,
            kind: c.kind,
            name: c.name,
            year: Int(c.year),
            poster: c.poster,
            titleId: c.titleId,
            inLibrary: c.inLibrary,
            asked: c.ask != nil
        )
    }

    static func score(_ s: TitleScore) -> TitleScoreInfo {
        TitleScoreInfo(source: s.source, text: TitleLogic.shared.scoreText(score: s))
    }

    static func file(_ f: OwnedFile?) -> TitleFileInfo? {
        guard let f else { return nil }
        return TitleFileInfo(
            id: f.id,
            fileName: f.fileName,
            size: f.size,
            addedAt: Format.date(iso: f.addedAt),
            resolutionLabel: f.resolutionLabel,
            edition: f.edition,
            isProtected: f.isProtected,
            audioLanguages: f.audioLanguages,
            subtitleLanguages: f.subtitleLanguages,
            epQuality: TitleLogic.shared.epQuality(file: f),
            group: f.group
        )
    }

    static func points(_ p: FilePoints?) -> PointsInfo? {
        guard let p else { return nil }
        let traits = p.traits.map { t -> String in
            let name = p.names[t.trait] ?? t.trait.replacingOccurrences(of: "own:", with: "").replacingOccurrences(of: "_", with: " ")
            return t.points >= 0 ? "\(name) +\(t.points)" : "\(name) \(t.points)"
        }
        return PointsInfo(
            step: p.step,
            goodEnoughAt: p.goodEnoughAt,
            stepReached: p.stepReached,
            points: Int(p.points),
            goodEnoughPoints: p.goodEnoughPoints?.intValue,
            goodEnough: p.goodEnough,
            meetsVersion: p.meetsVersion,
            traits: traits
        )
    }

    static func event(_ e: TitleEvent, tone: Any) -> HistoryRowInfo {
        HistoryRowInfo(
            id: e.id,
            summary: e.summary,
            at: Format.date(iso: e.at),
            episode: e.episode,
            version: e.version,
            tone: Mapping.tone(tone)
        )
    }

    static func version(_ v: OwnedVersion) -> TitleVersionInfo {
        TitleVersionInfo(
            id: v.id,
            label: v.label,
            main: v.main,
            pickRulesId: v.pickRulesId,
            pickRulesName: v.pickRulesName,
            libraryFolderId: v.libraryFolderId,
            folderPath: v.folderPath,
            keepUpToDate: v.keepUpToDate,
            edition: v.edition,
            audioLanguages: v.audioLanguages
        )
    }

    static func folder(_ f: LibraryFolder) -> FolderInfo {
        FolderInfo(id: f.id, path: f.path, shown: f.shown)
    }

    static func rules(_ r: PickRule) -> RulesInfo {
        RulesInfo(id: r.id, name: r.name)
    }

    static func title(_ s: TitleState) -> TitleInfo {
        let owned = s.owned
        let show = owned?.show
        let access = s.access
        let details = s.details
        let status = s.libraryStatus
        let askers = (owned?.asks ?? []).flatMap { ask in ask.people.isEmpty ? [ask.askedBy] : ask.people }.map(\.shown)
        var seenAskers = Set<String>()
        let uniqueAskers = askers.filter { !$0.isEmpty && seenAskers.insert($0).inserted }
        let outcome = s.outcome.map { o in
            OutcomeInfo(empty: o.empty, refused: o.refused, note: o.note, sources: o.sources.map { OutcomeSourceInfo(name: $0.name, why: $0.why) })
        }
        var points: [Int64: PointsInfo] = [:]
        for slot in s.pointSlots {
            if let value = s.points[slot], let mapped = Mapping.points(value) { points[slot.int64Value] = mapped }
        }
        let slotCards = s.slotCards.map { card in
            SlotCardInfo(
                slotId: card.slot.id,
                label: card.label,
                main: card.main,
                badges: card.badges,
                versionId: card.versionId,
                removable: card.removable,
                stateLabel: card.slot.stateLabel,
                leftInDownloader: TitleLogic.shared.leftInDownloader(slot: card.slot),
                file: Mapping.file(card.slot.file)
            )
        }
        let seasons = (show?.seasons ?? []).map { season in
            OwnedSeasonInfo(
                number: Int(season.number),
                name: season.name,
                keepUpToDate: season.keepUpToDate,
                episodes: Int(season.episodes),
                inLibrary: Int(season.inLibrary),
                missing: Int(season.missing),
                downloading: Int(season.downloading),
                failed: Int(season.failed),
                nextAir: season.nextAirDay.isEmpty ? season.nextAirDate : season.nextAirDay
            )
        }
        let facts = s.facts.enumerated().map { index, fact in
            TitleFactInfo(
                id: index,
                kind: FactKindValue(rawValue: key(fact.kind)) ?? .network,
                text: fact.text,
                number: fact.number,
                end: fact.end,
                season: Int(fact.season),
                episode: Int(fact.episode)
            )
        }
        let groups = s.actionGroups.map { group in group.compactMap { TitleActionKind(rawValue: key($0)) } }
        let downloads = s.activeDownloads.compactMap { slot -> TitleDownloadInfo? in
            guard let d = slot.download else { return nil }
            return TitleDownloadInfo(id: d.id, label: d.label.isEmpty ? slot.version : d.label, text: d.text, progress: d.progress)
        }
        return TitleInfo(
            load: load(s.status),
            problem: TitleProblemKind(rawValue: key(s.problem)) ?? TitleProblemKind.none,
            tab: TitleTabKind(rawValue: key(s.tab)) ?? .overview,
            kind: s.kind,
            tmdb: s.tmdb,
            ownedId: s.ownedId,
            name: s.name,
            year: Int(s.year),
            overview: s.overview,
            poster: s.poster,
            backdrop: s.backdrop,
            logo: s.logo,
            tagline: s.tagline,
            runtime: Int(s.runtime),
            seasonCount: Int(s.seasonCount),
            genres: s.genres,
            certification: s.certification,
            isShow: s.isShow,
            isOwned: s.isOwned,
            hasTmdb: s.hasTmdb,
            legacy: s.legacy,
            ready: s.ready,
            access: TitleAccessInfo(
                manage: access.manage,
                mayAsk: access.mayAsk,
                mayHide: access.mayHide,
                followOn: access.followOn,
                subtitlesOn: access.subtitlesOn,
                handsOff: access.handsOff,
                isKid: access.isKid,
                settings: access.settings
            ),
            libraryStatus: status.map {
                LibraryStatusInfo(
                    kind: LibraryKindValue(rawValue: key($0.kind)) ?? .missing,
                    label: LibraryLabelKind(rawValue: key($0.label)) ?? .notdownloaded,
                    count: Int($0.count),
                    total: Int($0.total),
                    tone: tone($0.tone)
                )
            },
            libraryAlert: s.libraryAlert,
            pill: HeroPillKind(rawValue: key(s.pill)) ?? HeroPillKind.none,
            askIsMine: s.askIsMine,
            canAdd: s.canAdd,
            canCheckReleases: s.canCheckReleases,
            askOffer: s.askOffer.flatMap { AskOfferKind(rawValue: key($0)) },
            fourKOffer: s.fourKOffer.flatMap { AskOfferKind(rawValue: key($0)) },
            primaryRuns: s.primaryRuns,
            actionGroups: groups,
            trailers: s.trailers.map { TitleVideoInfo(key: $0.key, name: $0.name, type: $0.type, language: $0.language) },
            trailerKey: s.trailer?.key ?? "",
            directors: s.directors.enumerated().map { person($1, index: $0) },
            starring: s.starring.enumerated().map { person($1, index: $0) },
            cast: s.cast.enumerated().map { person($1, index: $0) },
            credits: s.credits.enumerated().map { index, credit in
                TitlePersonInfo(
                    id: "\(index)-\(credit.person.name)",
                    personId: credit.person.id,
                    name: credit.person.name,
                    role: "",
                    character: credit.characters.first ?? credit.roles.first ?? "",
                    profile: credit.person.profile
                )
            },
            keywords: Array((details?.keywords ?? []).prefix(5)),
            contentNotes: details?.contentNotes ?? [],
            studio: s.isShow ? "" : (details?.studios.first ?? ""),
            network: show?.network ?? "",
            factScores: s.factScores.map(score),
            factTmdb: s.factTmdb > 0 ? TitleLogic.shared.oneDecimal(value: s.factTmdb) : "",
            scores: s.scores.map(score),
            tmdbScore: s.tmdbScore > 0 ? TitleLogic.shared.oneDecimal(value: s.tmdbScore) : "",
            awardBadges: s.awardBadges,
            hasAwards: !s.wiki.awards.isEmpty,
            wins: Int(s.wins),
            nominations: Int(s.nominations),
            collectionId: details?.collection?.id ?? "",
            collectionName: s.collectionName,
            collectionParts: s.collectionParts.map(card),
            similar: s.similar.map(card),
            moreFromPerson: s.moreFrom.person,
            moreFromItems: s.moreFromItems.map(card),
            leadName: s.leadPerson?.name ?? "",
            leadItems: s.leadItems.map(card),
            tmdbSeasons: s.isShow && s.hasTmdb && !(details?.seasons.isEmpty ?? true),
            facts: facts,
            links: s.links.map { TitleLinkInfo(name: $0.name, url: $0.url) },
            askers: uniqueAskers,
            outcome: outcome,
            following: s.following,
            followBusy: s.followBusy,
            watchLinks: s.watchLinks.map { WatchLinkInfo(server: $0.server, kind: $0.kind, url: $0.url) },
            ownedError: error(s.ownedError),
            noDownloader: owned?.noDownloader ?? "",
            statusSub: s.statusSub.flatMap { StatusSubKind(rawValue: key($0)) },
            fullSeasons: Int(s.fullSeasons),
            ownBadges: s.ownBadges,
            mainFile: file(s.mainFile),
            statusPercent: s.statusPercent?.intValue,
            statusButton: StatusButtonKind(rawValue: key(s.statusButton)) ?? StatusButtonKind.none,
            folderName: s.folderName,
            sizeBytes: owned?.sizeBytes ?? 0,
            pickRulesId: owned?.pickRulesId ?? 0,
            pickRulesName: owned?.pickRulesName ?? "",
            keepUpToDate: owned?.keepUpToDate ?? false,
            showStatus: show?.status ?? "",
            originalLanguage: owned?.originalLanguage ?? "",
            addedAt: owned.flatMap { Format.date(iso: $0.addedAt) },
            gateNote: s.gateNote,
            gateReason: owned?.gateReason ?? "",
            gateOpensAt: owned.flatMap { Format.date(iso: $0.gateOpensAt) },
            releaseGate: owned?.releaseGate ?? "",
            gateDelayDays: Int(owned?.gateDelayDays ?? 0),
            libraryFolderId: owned?.libraryFolderId ?? 0,
            slotCards: slotCards,
            versions: (owned?.versions ?? []).map(version),
            movieFileHeading: s.movieFileHeading,
            activeDownloads: downloads,
            history: s.history.map { event($0.event, tone: $0.tone) },
            heldCount: s.held.count,
            heldUntil: Format.date(iso: s.heldDaysLeft),
            ownedSeasons: seasons,
            seasonPick: s.seasonPick.flatMap { SeasonPickKind(rawValue: key($0)) },
            episodeOrder: show?.episodeOrder ?? "aired",
            episodeGroupId: show?.episodeGroup?.id ?? "",
            episodeGroupName: show?.episodeGroup?.name ?? "",
            packWindow: Int(show?.packWindow ?? 0),
            getAllMissing: s.getAllMissing,
            showOffHint: s.showOffHint,
            kindFolders: s.kindFolders.map(folder),
            kindRules: s.kindRules.map(rules),
            presetIds: s.qualityPresets.map(\.id),
            groups: (s.episodeGroups?.groups ?? []).map { GroupInfo(id: $0.id, name: $0.name, seasons: Int($0.seasons), episodes: Int($0.episodes)) },
            choicesLoaded: s.choicesLoaded,
            choicesError: error(s.choicesError),
            renameCount: Int(s.renameCount),
            leftoverCount: Int(s.leftoverCount),
            points: points,
            users: s.users.map { UserInfo(id: $0.id, name: $0.name) },
            hides: s.hides.map { HideInfo(id: $0.id, genre: $0.genre, name: $0.name, userId: $0.userId) },
            busy: s.busy,
            feedback: feedback(s.feedback)
        )
    }

    static func episode(_ e: OwnedEpisode, today: String) -> EpisodeInfo {
        let slot = e.mainSlot
        return EpisodeInfo(
            id: e.id,
            season: Int(e.season),
            episode: Int(e.episode),
            name: e.name,
            airDay: e.airDay.isEmpty ? e.airDate : e.airDay,
            minutes: Int(e.minutes),
            still: e.still,
            keepUpToDate: e.keepUpToDate,
            tone: EpisodeToneKind(rawValue: key(TitleLogic.shared.episodeTone(episode: e))) ?? .missing,
            notAired: TitleLogic.shared.notAired(episode: e, today: today),
            slotId: slot?.id,
            stateLabel: slot?.stateLabel ?? "",
            reason: slot?.reason ?? "",
            slotState: slot?.state ?? "",
            points: slot?.points?.intValue,
            goodEnoughPoints: slot?.goodEnoughPoints?.intValue,
            file: file(slot?.file)
        )
    }

    static func seasons(_ s: ShowSeasonsState, today: String) -> ShowSeasonsInfo {
        var loaded: [Int: SeasonEpisodesInfo] = [:]
        var shown: [Int: [EpisodeInfo]] = [:]
        var history: [Int: [HistoryRowInfo]] = [:]
        var count: [Int: Int] = [:]
        var size: [Int: Int64] = [:]
        for value in s.loaded {
            let n = Int(value.season)
            loaded[n] = SeasonEpisodesInfo(loading: value.loading, failed: value.failed, episodes: value.episodes.map { episode($0, today: today) })
            shown[n] = s.episodesOf(season: value.season).map { episode($0, today: today) }
            history[n] = s.seasonHistory(season: value.season).map { event($0, tone: TitleLogic.shared.eventTone(kind: $0.kind)) }
            count[n] = s.seasonFiles(season: value.season).count
            size[n] = s.seasonSize(season: value.season)
        }
        return ShowSeasonsInfo(
            open: Set(s.openSeasons.map { Int($0.int32Value) }),
            seasons: loaded,
            shown: shown,
            outside: s.outside.map { episode($0, today: today) },
            belowOnly: s.belowOnly,
            belowCount: Int(s.belowCount),
            busy: s.busy,
            packBusy: s.packBusy,
            historyLoaded: s.historyLoaded,
            seasonHistory: history,
            filesCount: count,
            filesSize: size,
            feedback: feedback(s.feedback)
        )
    }

    static func episodeSheet(_ s: EpisodeState, today: String) -> EpisodeSheetInfo {
        EpisodeSheetInfo(
            episode: episode(s.episode, today: today),
            manage: s.manage,
            subtitlesOn: s.subtitlesOn,
            synopsis: s.synopsis,
            events: s.events.map { event($0, tone: TitleLogic.shared.eventTone(kind: $0.kind)) },
            eventsLoaded: s.eventsLoaded,
            points: points(s.points),
            busy: s.busy,
            code: s.code,
            downloading: s.downloading,
            chosenWhy: s.chosenWhy,
            feedback: feedback(s.feedback)
        )
    }

    static func release(_ r: TitleRelease) -> ReleaseInfo {
        let verdict = TitleLogic.shared.verdict(release: r)
        return ReleaseInfo(
            id: r.id,
            name: r.name,
            size: r.size,
            seeders: r.seeders?.intValue,
            protocolName: r.protocolName,
            state: r.state,
            sourceName: r.sourceName,
            infoURL: r.infoUrl,
            publishedAt: Format.date(iso: r.publishedAt),
            passed: r.passed,
            points: Int(r.points),
            needsForce: r.needsForce,
            verdict: VerdictKindValue(rawValue: key(verdict.kind)) ?? .judged,
            verdictTone: tone(verdict.tone),
            recommended: TitleLogic.shared.recommended(release: r),
            tags: TitleLogic.shared.readTags(release: r),
            keyReasons: TitleLogic.shared.keyReasons(reasons: r.reasons).map(\.text),
            reasons: r.reasons.map(\.text),
            group: r.read?.group ?? ""
        )
    }

    static func releases(_ s: ReleasesState) -> ReleasesInfo {
        let last = s.page?.lastSearch
        return ReleasesInfo(
            load: load(s.status),
            scope: ReleaseScopeValue(rawValue: key(s.scope.kind)) ?? .whole,
            season: Int(s.scope.season),
            episode: Int(s.scope.episode),
            titleName: s.titleName,
            check: s.check,
            wholeShow: s.wholeShow,
            running: s.running,
            allTab: s.allTab,
            total: Int(s.total),
            allCount: s.all.count,
            goodCount: Int(s.goodCount),
            sort: ReleaseSortKind(rawValue: key(s.sort)) ?? .best,
            resolution: Int(s.resolution),
            source: s.source,
            language: s.language,
            query: s.query,
            resolutions: s.resolutions.map { Int($0.int32Value) },
            sources: s.sources,
            languages: s.languages,
            shown: s.shown.map(release),
            empty: ReleaseEmptyKind(rawValue: key(s.empty)) ?? ReleaseEmptyKind.none,
            lastSearchAt: last.flatMap { Format.date(iso: $0.at) },
            droppedSources: last?.droppedSources ?? [],
            dropped: Int(last?.dropped ?? 0),
            rulesChanged: s.page?.rulesChanged ?? false,
            canShowMore: s.canShowMore,
            armed: Set(s.armed.map { $0.int64Value }),
            sent: Set(s.sent.map { $0.int64Value }),
            busy: s.busy,
            rules: s.rules.map(rules),
            rulesId: s.rulesId,
            checkId: s.checkId,
            pickedId: s.picked?.id,
            done: s.done,
            feedback: feedback(s.feedback)
        )
    }

    static func add(_ s: AddTitleState) -> AddTitleInfo {
        AddTitleInfo(
            kind: s.kind,
            name: s.name,
            manage: s.manage,
            handsOff: s.handsOff,
            choose: s.choose,
            load: load(s.status),
            defaultsLate: s.defaultsLate,
            folders: s.folders.map(folder),
            rules: s.rules.map(rules),
            folderId: s.folderId,
            rulesId: s.rulesId,
            gate: s.gate,
            keepUpToDate: s.keepUpToDate,
            searchOnAdd: s.searchOnAdd,
            secondOn: s.secondOn,
            secondLabel: s.secondLabel,
            secondRulesId: s.secondRulesId,
            secondFolderId: s.secondFolderId,
            follow: s.follow,
            seasons: s.seasons,
            newSeasons: s.newSeasons,
            order: s.order,
            source: s.source,
            submitting: s.submitting,
            error: error(s.error),
            addedId: s.addedId,
            openPick: s.openPick,
            batch: s.batch.map { item in
                BatchItemInfo(
                    tmdbId: item.card.tmdbId,
                    name: item.card.name,
                    year: Int(item.card.year),
                    line: BatchLineKind(rawValue: key(item.line)) ?? .waiting,
                    error: error(item.error)
                )
            },
            batchDone: s.batchDone,
            isBatch: s.isBatch,
            noFolder: s.noFolder,
            secondProblem: s.secondProblem.flatMap { LabelProblemKind(rawValue: key($0)) },
            canSubmit: s.canSubmit,
            sameFolder: s.sameFolder,
            newSeasonsLocked: s.newSeasonsLocked,
            feedback: feedback(s.feedback)
        )
    }

    static func want(_ w: SubtitleWant) -> SubtitleWantInfo {
        SubtitleWantInfo(language: w.language, hearingImpaired: w.hearingImpaired, forced: w.forced, translateFrom: w.translateFrom)
    }

    static func subtitleRow(_ r: SubtitleRowView) -> SubtitleRowInfo {
        SubtitleRowInfo(
            id: r.id,
            language: r.language,
            forced: r.forced,
            hearingImpaired: r.hearingImpaired,
            place: r.place,
            codec: r.codec,
            relativePath: r.relativePath,
            site: r.site,
            score: r.score?.intValue,
            origin: r.origin,
            state: r.state,
            savedAt: Format.date(iso: r.savedAt),
            tries: Int(r.tries),
            nextTryAt: Format.date(iso: r.nextTryAt),
            made: r.made,
            picture: r.picture,
            ours: TitleLogic.shared.ourSubtitle(row: r)
        )
    }

    static func subtitles(_ s: SubtitlesState) -> SubtitlesInfo {
        let files = s.files.map { file -> SubtitleFileInfo in
            let missing = s.missing(file: file)
            var translatable: [String: [SubtitleRowInfo]] = [:]
            for row in missing {
                translatable[row.language] = s.translatable(file: file, language: row.language).map(subtitleRow)
            }
            return SubtitleFileInfo(
                fileId: file.fileId,
                versionLabel: file.versionLabel,
                fileName: file.fileName,
                present: s.present(file: file).map(subtitleRow),
                missing: missing.map(subtitleRow),
                translatable: translatable
            )
        }
        let choices = s.choices.map { c in
            SubtitleChoicesInfo(
                fileId: c.fileId,
                loading: c.loading,
                sitesAsked: c.choices?.sitesAsked ?? [],
                outcomes: (c.choices?.outcomes ?? []).enumerated().map { index, outcome in
                    SubtitleOutcomeInfo(
                        id: index,
                        want: want(outcome.want),
                        result: outcome.result,
                        coveredBeside: outcome.covered.contains { $0.place == "beside" },
                        coveredPicture: outcome.covered.contains { $0.picture },
                        covered: !outcome.covered.isEmpty,
                        choices: outcome.choices.map { candidate in
                            SubtitleCandidateInfo(
                                site: candidate.site,
                                key: candidate.key,
                                name: candidate.shownName,
                                hearingImpaired: candidate.hearingImpaired,
                                forced: candidate.forced,
                                trusted: candidate.trusted,
                                downloads: Int(candidate.downloads),
                                machineMade: !candidate.machineMade.isEmpty,
                                score: Int(candidate.score.total),
                                refused: !candidate.score.refused.isEmpty
                            )
                        }
                    )
                }
            )
        }
        return SubtitlesInfo(
            load: load(s.status),
            itemId: s.itemId,
            movie: s.movie,
            listMode: s.listMode,
            mode: SubtitleModeKind(rawValue: key(s.mode)) ?? .default,
            wish: s.wish.map(want),
            wishFrom: s.wishFrom,
            wishCodes: s.wishCodes,
            missingCount: Int(s.missingCount),
            canCleanAll: s.canCleanAll,
            files: files,
            languages: s.languages.map { SubtitleLanguageInfo(code: $0.code, name: $0.name) },
            translator: s.translator,
            editing: s.editing,
            editOwn: s.editOwn,
            editWants: s.editWants.map(want),
            editProblem: s.editProblem.flatMap { WishProblemKind(rawValue: key($0)) },
            choices: choices,
            busy: s.busy,
            feedback: feedback(s.feedback)
        )
    }

    static func leftover(_ l: TitleLeftover) -> LeftoverInfo {
        LeftoverInfo(
            fileId: l.fileId,
            fileName: l.fileName,
            size: l.size,
            resolution: Int(l.resolution),
            reason: l.reason,
            droppedSeason: Int(l.droppedSeason)
        )
    }

    static func leftovers(_ s: LeftoversState) -> LeftoversInfo {
        let match = s.match
        let shift = s.shift
        let code: (Int32, Int32) -> String = { TitleLogic.shared.episodeCode(season: $0, episode: $1) }
        return LeftoversInfo(
            load: load(s.status),
            show: s.show,
            handsOff: s.handsOff,
            open: s.open.map(leftover),
            leftAlone: s.leftAlone.map(leftover),
            episodesLoaded: s.episodesLoaded,
            seasonNumbers: s.seasonNumbers.map { Int($0.int32Value) },
            matchFileId: match?.leftover.fileId,
            matchName: match?.leftover.fileName ?? "",
            matchSeason: Int(match?.season ?? 0),
            matchPicked: Set((match?.picked ?? []).map { $0.int64Value }),
            matchEpisodes: match.map { m in
                s.episodesIn(season: m.season).map { MatchEpisodeInfo(id: $0.id, season: Int($0.season), episode: Int($0.episode), name: $0.name, hasFile: $0.hasFile) }
            } ?? [],
            pickedHasFile: s.pickedHasFile,
            shiftFrom: Int(shift?.fromSeason ?? 0),
            shiftTo: Int(shift?.toSeason ?? 0),
            shiftBy: Int(shift?.shift ?? 0),
            shiftMoves: (shift?.moves ?? []).map {
                ShiftMoveInfo(fileId: $0.fileId, from: code($0.fromSeason, $0.fromEpisode), to: code($0.toSeason, $0.toEpisode))
            },
            rescanning: s.rescanning,
            busy: s.busy,
            renameOffered: s.renameOffered,
            feedback: feedback(s.feedback)
        )
    }

    static func renameStep(_ r: TitleRenameStep) -> RenameStepInfo {
        RenameStepInfo(
            fileId: r.fileId,
            from: r.from,
            to: r.to,
            skip: r.skip,
            subtitles: r.subtitles.map { $0.to.isEmpty ? "\($0.from) · \($0.skip)" : "\($0.from) → \($0.to)" }
        )
    }

    static func rename(_ s: RenameState) -> RenameInfo {
        RenameInfo(
            load: load(s.status),
            season: Int(s.season),
            changes: s.changes.map(renameStep),
            skipped: s.skipped.map(renameStep),
            alreadyRight: Int(s.alreadyRight),
            running: s.running,
            progress: s.progress,
            note: s.note,
            done: s.done,
            feedback: feedback(s.feedback)
        )
    }

    static func remove(_ s: RemoveState) -> RemoveInfo {
        let plan = s.plan
        return RemoveInfo(
            name: s.name,
            show: s.show,
            season: Int(s.season),
            episodeCount: s.episodeIds.count,
            handsOff: s.handsOff,
            whole: s.whole,
            album: s.album,
            artist: s.artist,
            deleteFiles: s.deleteFiles,
            neverAdd: s.neverAdd,
            hasPlan: plan != nil,
            planScope: plan?.scope ?? "",
            planSeason: Int(plan?.season ?? 0),
            planEpisodes: plan?.episodes ?? [],
            heldCount: plan?.held.count ?? 0,
            keptCount: plan?.kept.count ?? 0,
            holdingDays: Int(plan?.holdingDays ?? 0),
            activeDownloads: Int(plan?.activeDownloads ?? 0),
            paths: Array(((plan?.held ?? []) + (plan?.kept ?? [])).prefix(20).map { $0.why.isEmpty ? $0.path : "\($0.path) · \($0.why)" }),
            planError: error(s.planError),
            running: s.running,
            done: s.done,
            ready: s.ready,
            canUndo: !s.heldIds.isEmpty && s.whole,
            feedback: feedback(s.feedback)
        )
    }

    static func seasonBrowser(_ s: SeasonBrowserState) -> SeasonBrowserInfo {
        SeasonBrowserInfo(
            seasons: s.ordered.map { TMDBSeasonInfo(number: Int($0.number), episodeCount: Int($0.episodeCount), year: Int($0.year)) },
            selected: Int(s.selected),
            newSeason: s.newSeason?.intValue,
            episodes: s.shown.map { e in
                TMDBEpisodeInfo(
                    number: Int(e.number),
                    name: e.name,
                    overview: e.overview,
                    runtime: Int(e.runtime),
                    still: e.still,
                    airDay: e.airDay,
                    absolute: Int(s.absolute(episode: e.number))
                )
            },
            loading: s.loading,
            failed: s.failed
        )
    }

    static func reviews(_ s: ReviewsState) -> ReviewsInfo {
        ReviewsInfo(
            load: load(s.status),
            visible: s.visible,
            score: s.data.score,
            votes: Int(s.data.votes),
            watching: Int(s.data.watching),
            sources: s.data.sources.map { ReviewSourceInfo(id: $0.id, name: $0.name) },
            source: s.source,
            order: s.order,
            canHelpful: s.canHelpful,
            reviews: s.reviews.enumerated().map { index, r in
                ReviewInfo(id: index, author: r.author, date: r.date, rating: r.rating, text: r.text, likes: Int(r.likes), spoiler: r.spoiler)
            },
            hasMore: s.hasMore,
            loadingMore: s.loadingMore
        )
    }

    static func trailer(_ t: TrailerSource?) -> TrailerSourceInfo? {
        guard let t else { return nil }
        return TrailerSourceInfo(hls: t.hls, mp4: t.mp4, relay: t.relay, youTubeURL: t.youTubeUrl, playable: t.playable)
    }

    static func kotlinSeasonPick(_ p: SeasonPickKind) -> SeasonPick {
        switch p {
        case .all: SeasonPick.all
        case .latest: SeasonPick.latest
        case .off: SeasonPick.off
        case .custom: SeasonPick.custom
        }
    }

    static func kotlinSort(_ s: ReleaseSortKind) -> ReleaseSort {
        switch s {
        case .best: ReleaseSort.best
        case .points: ReleaseSort.points
        case .quality: ReleaseSort.quality
        case .size: ReleaseSort.size
        case .seeders: ReleaseSort.seeders
        case .age: ReleaseSort.age
        }
    }

    static func kotlinScope(_ kind: ReleaseScopeValue) -> ReleaseScopeKind {
        switch kind {
        case .whole: ReleaseScopeKind.whole
        case .season: ReleaseScopeKind.season
        case .episode: ReleaseScopeKind.episode
        case .check: ReleaseScopeKind.check
        }
    }

    static func kotlinTab(_ tab: TitleTabKind) -> TitleTab {
        switch tab {
        case .overview: TitleTab.overview
        case .library: TitleTab.library
        }
    }
}
