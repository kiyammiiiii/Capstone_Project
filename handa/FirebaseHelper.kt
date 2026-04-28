package com.labactivity.handa

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FirebaseHelper(private val context: Context) {

    private val databaseReference = FirebaseDatabase.getInstance().getReference("QC_hotlines")

    fun sendBarangaysToFirebase() {
        // Check if the barangays data already exists
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    uploadBarangays()
                } else {
                    Log.d("FirebaseHelper", "Barangay data already exists, skipping upload")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseHelper", "Failed to check barangay data", error.toException())
            }
        })
    }

    private fun uploadBarangays() {
        val barangays = listOf(
            "Alicia", "Bagong Pag Asa", "Bahay Toro", "Balingasa", "Bungad",
            "Damar", "Damayan", "Del Mante", "Katipunan", "Lourdes",
            "Maharlika", "Manresa", "Mariblo", "Masambong", "NS Amoranto",
            "Nayong Kanluran", "Paang Bundok", "Pag-ibig Sa Nayon", "Pattok",
            "Paraiso", "Phi-Am", "Project 6", "Ramon Magsaysay", "Salvacion",
            "San Antonio", "San Isidro Labrador", "San Jose", "Siena",
            "St Peter", "Sta Cruz", "Sta Teresita", "Sto Cristo",
            "Sto Domingo", "Talayan", "Vasra", "Veterans Village", "West Triangle",
            "Amihan", "Bagumbayan", "Bagumbuhay", "Bayanihan", "Blue Ridge A",
            "Blue Ridge B", "Camp Aguinaldo", "Dioquino Zobel", "Duyan Duyan",
            "E Rodriguez", "East Kamias", "Escopa I", "Escopa II", "Escopa III",
            "Escopa IV", "Libis", "Loyola Heights", "Mangga", "Marilag",
            "Masagana", "Matandang Balara", "Milagrosa", "Pansol", "Qurino 2-A",
            "Quirino 2-8", "Quirino 2-C", "Quirino 3-A", "Quirino 3-B (Claro)",
            "San Roque", "Silangan", "Socorro", "St Ignalius", "Tagumpay",
            "Ugong Norte", "Villa Maria Clara", "West Kamas", "White Plains",
            "BJ Crame", "Bolocan", "Central", "Damayang Lagi", "Don Manuel",
            "Dona Aurora", "Dona Imelda", "Dona Josefa", "Horseshoe",
            "Imma Concepcion", "Kalusugan", "Kamuning", "Kaunlaran",
            "Kristong Hari", "Krus Na Ligas", "Laging Handa", "Malaya",
            "Mariana", "Obrero", "Old Capitol Site", "Paligsahan",
            "Pinagkaisahan", "Pinyahan", "Roxas", "Sacred Heart",
            "San Isidro Galas", "San Martin De Porres", "San Vicente",
            "Santol", "Sikatuna Village", "South Triangle", "Sto Nino",
            "Tatalon", "Teachers Village East", "Teachers Village West",
            "UP Campus", "UP Village", "Valencia",
            "Bagbag", "Capri", "Fairview", "Greater Lagro",
            "Gulod", "Kaligayahan", "Nagkaisang Nayon", "North Fairview",
            "Novaliches Proper", "Pasong Putik Proper", "San Agustin", "San Bartolome",
            "Sta Lucia", "Sta Monica", "Apolonio Samson", "Baesa",
            "Balon Balo", "Culiat", "New Era", "Pasong Tamo",
            "Sangandaan", "Sauyo", "Talipapa", "Tandang Sora", "Unang Sigaw"
        )

        for (barangay in barangays) {
            val sanitizedBarangay = barangay.replace(Regex("[.#$\\[\\]]"), "_")
        }
    }
}
