Parcels
A Parcel is the name of a claimed 3-dimensional space. A Parcel can be of any size and can be placed at any block position, given that it does not overlap another Parcel's space (currently only the Overworld is supported).


Creating / Claiming
A Deed is used to place a potential Parcel into the world and also to claim it. Ops can add Parcels directly into the world by commands.



Types
There are 4 types of Parcels:

Player Parcel
The Player Parcel is the default/standard Parcel. It can be placed anywhere in the world, except in Nation Parcels that have Closed borders.
Nation Parcel
A Nation Parcel is a (typically) large, special Parcel. The Nation Owner can embed Zone and Citizen Parcels within it. Nations can have Open or Closed Borders. Open borders grant access to any Player or Citizen Deed to claim or place Parcels in the designated Zones. Closed borders restricts access to only Citizen Deeds of the Nation.
Zone Parcel
A Zone Parcel is a special Parcel that can only be placed inside a Nation Parcel. It is used to designate areas where Players can claim land within the Nation (using Citizen Deeds, or Player Deeds if the Nation has Open borders). Zone are added to Nation Parcels using Commands or the Zone Placement Tool by the Nation Owner.
Citizen Parcel
A Citizen Parcel is a special Parcel that can only be placed inside a Nation or Zone Parcel. Only the Nation owner can add Citizen Parcels directly to a Nation Parcel (ie not inside a Zone) using Commands or the Citizen Placement Tool.


Ownership
Parcels can only have one owner. Owners have access to all the Parcel Commands, and can perform any action (break, place, etc) within the Parcel space. A Parcel can have a whitelist, in which the whitelisted Players have the same access (break, place etc) in the Parcel space as the Owner.



Abandoned Parcels
Owners can abandon their Parcels by using a Command. The Parcel will still exist in the world, but simply will not have an owner. Thus anyone will be able to perform any action (break, place, etc) in that space until it is claimed by a Player. Abandoned Parcels can be claimed by using a Deed of the same type and of equal or greater Parcel size (measured by x * y * z).

Player Deed can claim Player Parcel
Player Deed can claim Citizen Parcel (Open borders)
Any Citizen Deed can claim Citizen Parcel (Open borders)
Citizen Deed can claim Citizen Parcel of the same Nation (Closed borders)
Nation Deed can claim Nation Parcel
