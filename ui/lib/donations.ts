export interface DonationsFile {
    top: DonationEntry[],
    recent: DonationEntry[]
}

export interface DonationEntry {
    donor_name: string
    amount: number
    character_id: number
    corporation_id: number
}
