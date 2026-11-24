import SwiftUI

struct MusicArtistAboutCard: View {
    let info: MusicArtistInfo
    let onOpen: () -> Void

    private var line: String {
        [
            MusicArtistText.listeners(info),
            MusicArtistText.activeYears(info),
            info.genres.prefix(2).joined(separator: ", "),
        ]
        .filter { !$0.isEmpty }
        .joined(separator: " · ")
    }

    private var bio: String {
        MusicArtistText.sentences(info.bio?.text ?? "", limit: 2)
    }

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                header
                if !line.isEmpty {
                    Text(line)
                        .font(.subheadline)
                        .foregroundStyle(Theme.textMuted)
                        .multilineTextAlignment(.leading)
                }
                if !bio.isEmpty {
                    Text(bio)
                        .font(.subheadline)
                        .foregroundStyle(Theme.text)
                        .lineLimit(4)
                        .multilineTextAlignment(.leading)
                }
                Text(L10n.format("music.artist.about.more", info.name))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.accentText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var header: some View {
        HStack(spacing: Theme.Spacing.medium) {
            MusicArt(path: info.image.isEmpty ? info.wide : info.image, name: info.name, round: true, size: 250)
                .frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 2) {
                Text(L10n.format("music.artist.about.title", info.name))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .multilineTextAlignment(.leading)
                if info.rank > 0 {
                    Text(L10n.format("music.artist.stats.rank", info.rank))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }
}

struct MusicArtistAboutSheet: View {
    let info: MusicArtistInfo
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                    header
                    facts
                    bio
                    genres
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(Theme.Spacing.screen)
            }
            .navigationTitle(L10n.format("music.artist.about.title", info.name))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("title.close")) { dismiss() }
                }
            }
        }
    }

    private var header: some View {
        HStack(spacing: Theme.Spacing.medium) {
            MusicArt(path: info.image.isEmpty ? info.wide : info.image, name: info.name, round: true, size: 250)
                .frame(width: 88, height: 88)
            VStack(alignment: .leading, spacing: 2) {
                Text(info.name)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Theme.text)
                if info.rank > 0 {
                    Text(L10n.format("music.artist.stats.rank", info.rank))
                        .font(.subheadline)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    private var facts: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.listeners > 0 { row(L10n.tr("music.artist.about.listeners"), info.listeners.formatted()) }
            if info.change != 0 { row(L10n.tr("music.artist.about.today"), MusicArtistText.change(info.change)) }
            if info.albumCount > 0 { row(L10n.tr("music.artist.about.albums"), "\(info.albumCount)") }
            if info.singleCount > 0 { row(L10n.tr("music.artist.about.singles"), "\(info.singleCount)") }
            if !info.country.isEmpty { Text(L10n.format("music.artist.about.from", MusicArtistText.country(info.country))).font(.subheadline) }
            if !info.activeSince.isEmpty { Text(MusicArtistText.activeYears(info)).font(.subheadline) }
            row(L10n.tr("music.artist.about.kind"), MusicArtistText.kind(info.kind))
        }
        .foregroundStyle(Theme.text)
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
            Spacer(minLength: Theme.Spacing.medium)
            Text(value)
                .font(.subheadline)
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.trailing)
        }
    }

    @ViewBuilder
    private var bio: some View {
        if let bio = info.bio {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                ForEach(paragraphs(bio.text), id: \.self) { paragraph in
                    Text(paragraph)
                        .font(.body)
                        .foregroundStyle(Theme.text)
                }
                credits(bio)
            }
        }
    }

    private func paragraphs(_ text: String) -> [String] {
        text.components(separatedBy: "\n\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    @ViewBuilder
    private func credits(_ bio: MusicBioInfo) -> some View {
        if !bio.attribution.isEmpty {
            Text(L10n.format("music.artist.bio.source", bio.attribution))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        if !bio.license.isEmpty {
            if let url = URL(string: bio.licenseUrl), !bio.licenseUrl.isEmpty {
                Link(destination: url) {
                    Text(L10n.format("music.artist.bio.licence", bio.license)).font(.footnote)
                }
            } else {
                Text(L10n.format("music.artist.bio.licence", bio.license))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        if let url = URL(string: bio.url), !bio.url.isEmpty {
            Link(destination: url) {
                Text(L10n.tr("music.artist.bio.read")).font(.subheadline.weight(.semibold))
            }
        }
    }

    @ViewBuilder
    private var genres: some View {
        if !info.genres.isEmpty {
            MusicArtistFlowLayout {
                ForEach(info.genres, id: \.self) { genre in
                    MusicArtistChip(text: genre)
                }
            }
        }
    }
}
