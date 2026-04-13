function RatingCard({ label }) {
  return (
    <div>
      <div className="stars">
        {Array.from({ length: 5 }).map((_, i) => (
          <svg key={i} className="star" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 17.3l-6.18 3.73 1.64-7.03L2 9.24l7.19-.61L12 2l2.81 6.63L22 9.24l-5.46 4.76 1.64 7.03z" />
          </svg>
        ))}
      </div>
      <p>{label}</p>
    </div>
  );
}

export default RatingCard;