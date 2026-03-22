import { Card, CardTitle, CardHeader, CardContent } from '@/components/ui/card'
import { User, Mail, MapPin, Globe, Calendar, Shield } from 'lucide-react'
import React from 'react'
import { useSelector } from 'react-redux'

function Profile() {
    const auth = useSelector(state => state.auth);

    const infoItems = [
      { label: 'Email', value: auth.user?.email, icon: Mail },
      { label: 'Full Name', value: auth.user?.fullName, icon: User },
      { label: 'Date of Birth', value: '25/09/1919', icon: Calendar },
      { label: 'Nationality', value: 'Indian', icon: Globe },
      { label: 'Address', value: 'Tadkal', icon: MapPin },
      { label: 'City', value: 'Hyderabad', icon: MapPin },
      { label: 'Passcode', value: '••••••', icon: Shield },
      { label: 'Country', value: 'India', icon: Globe },
    ];

    return (
        <div className='space-y-6'>
            {/* Header */}
            <div>
              <h1 className='text-2xl font-bold'>Profile</h1>
              <p className='text-sm text-muted-foreground mt-1'>Your account information</p>
            </div>

            {/* Profile Card */}
            <div className='glass-card rounded-2xl overflow-hidden'>
                {/* Profile banner */}
                <div className='h-24 gradient-primary relative'>
                  <div className='absolute -bottom-8 left-6'>
                    <div className='w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-2xl font-bold text-white ring-4 ring-background shadow-xl'>
                      {auth.user?.fullName?.[0]?.toUpperCase() || 'U'}
                    </div>
                  </div>
                </div>

                <div className='pt-12 px-6 pb-6'>
                  <h2 className='text-lg font-bold'>{auth.user?.fullName || 'User'}</h2>
                  <p className='text-sm text-muted-foreground'>{auth.user?.email}</p>
                </div>

                {/* Info grid */}
                <div className='px-6 pb-6'>
                  <div className='grid grid-cols-1 md:grid-cols-2 gap-4'>
                    {infoItems.map((item) => {
                      const Icon = item.icon;
                      return (
                        <div key={item.label} className='flex items-center gap-3 p-3 rounded-xl bg-secondary/30'>
                          <div className='p-2 rounded-lg bg-secondary/60'>
                            <Icon className='w-4 h-4 text-muted-foreground' />
                          </div>
                          <div>
                            <p className='text-[10px] text-muted-foreground uppercase tracking-wider'>{item.label}</p>
                            <p className='text-sm font-medium'>{item.value || '—'}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
            </div>
        </div>
    )
}

export default Profile
